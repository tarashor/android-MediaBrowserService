/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.mediasession.service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.example.android.mediasession.service.contentcatalogs.MusicLibrary;
import com.example.android.mediasession.service.notifications.MediaNotificationManager;
import com.example.android.mediasession.service.players.MediaPlayerAdapter;

import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {

    private static final String TAG = MusicService.class.getSimpleName();

    private MediaSessionCompat mSession;
    private PlayerAdapter mPlayback;
    private MediaNotificationManager mMediaNotificationManager;
    private MediaSessionCallback mCallback;
    private boolean mServiceInStartedState;

    private final IBinder localBinder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();

        // Create a new MediaSession.
        mSession = new MediaSessionCompat(this, "MusicService");
        mCallback = new MediaSessionCallback();
        mSession.setCallback(mCallback);
        mSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        //setSessionToken(mSession.getSessionToken());

        mMediaNotificationManager = new MediaNotificationManager(this);

        mPlayback = new MediaPlayerAdapter(this, new MediaPlayerListener());
        Log.d(TAG, "onCreate: MusicService creating MediaSession, and MediaNotificationManager");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        //Note: Once the service is started, it must start to run in the foreground.
        //https://developer.android.com/reference/android/support/v4/media/session/MediaButtonReceiver.html
        moveServiceToStartedState(mSession.getController().getPlaybackState());
        //FUCK
        MediaButtonReceiver.handleIntent(mSession, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        mMediaNotificationManager.onDestroy();
        mPlayback.stop();
        mSession.release();
        Log.d(TAG, "onDestroy: MediaPlayerAdapter stopped, and MediaSession released");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    public MediaSessionCompat.Token getMediaSessionToken() {
        return mSession.getSessionToken();
    }


    public static Intent getIntent(Context mContext) {
        return new Intent(mContext, MusicService.class);
    }

    // MediaSession Callback: Transport Controls -> MediaPlayerAdapter
    public class MediaSessionCallback extends MediaSessionCompat.Callback {
        private MediaMetadataCompat mPreparedMedia;


        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
            mPreparedMedia = MusicLibrary.getMetadata(MusicService.this, mediaId);
            mSession.setMetadata(mPreparedMedia);
            mPlayback.playFromMedia(mPreparedMedia);

            if (!mSession.isActive()) {
                mSession.setActive(true);
            }
        }

        @Override
        public void onPlay() {
            if (mPreparedMedia == null) {
                onPrepare();
            }

            mPlayback.playFromMedia(mPreparedMedia);
            Log.d(TAG, "onPlayFromMediaId: MediaSession active");
        }

        @Override
        public void onPause() {
            mPlayback.pause();
        }

        @Override
        public void onStop() {
            mPlayback.stop();
            mSession.setActive(false);
        }

        @Override
        public void onSeekTo(long pos) {
            mPlayback.seekTo(pos);
        }


    }

    private void moveServiceToStartedState(PlaybackStateCompat state) {
        Notification notification =
                mMediaNotificationManager.getNotification(
                        mPlayback.getCurrentMedia(), state, mSession.getSessionToken());

        if (!mServiceInStartedState) {
            ContextCompat.startForegroundService(
                    MusicService.this, getIntent(MusicService.this));
            mServiceInStartedState = true;
        }

        startForeground(MediaNotificationManager.NOTIFICATION_ID, notification);
    }

    private void updateNotificationForPause(PlaybackStateCompat state) {
        stopForeground(false);
        Notification notification =
                mMediaNotificationManager.getNotification(
                        mPlayback.getCurrentMedia(), state, mSession.getSessionToken());
        mMediaNotificationManager.getNotificationManager()
                .notify(MediaNotificationManager.NOTIFICATION_ID, notification);
    }

    private void moveServiceOutOfStartedState(PlaybackStateCompat state) {
        stopForeground(true);
        stopSelf();
        mServiceInStartedState = false;
    }

    // MediaPlayerAdapter Callback: MediaPlayerAdapter state -> MusicService.
    public class MediaPlayerListener extends PlaybackInfoListener {

        @Override
        public void onPlaybackStateChange(PlaybackStateCompat state) {
            // Report the state to the MediaSession.
            mSession.setPlaybackState(state);

            // Manage the started state of this service.
            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    moveServiceToStartedState(state);
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                    updateNotificationForPause(state);
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    moveServiceOutOfStartedState(state);
                    break;
            }
        }


    }

    public class LocalBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }
}