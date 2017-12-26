package com.example.android.mediasession.ui;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.mediasession.R;
import com.example.android.mediasession.service.contentcatalogs.MusicLibrary;

import java.util.List;

/**
 * Created by tarasgoriachko on 26.12.17.
 */

public class TracksAdapter extends RecyclerView.Adapter {
    private final List<MediaBrowserCompat.MediaItem> items;
    private final View.OnClickListener onClickListener;

    public TracksAdapter(View.OnClickListener onClickListener){
        this.onClickListener = onClickListener;
        items =  MusicLibrary.getMediaItems();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.track, parent, false);
        view.setOnClickListener(onClickListener);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        TrackViewHolder trackViewHolder = (TrackViewHolder)holder;
        trackViewHolder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static class TrackViewHolder extends RecyclerView.ViewHolder{

        private TextView trackNameTextView;

        public TrackViewHolder(View itemView) {
            super(itemView);
            trackNameTextView = itemView.findViewById(R.id.track_name_txt);
        }

        public void bind(MediaBrowserCompat.MediaItem mediaItem){
            String mediaId = mediaItem.getDescription().getMediaId();
            String title = MusicLibrary.getMetadata(itemView.getContext(), mediaId).getString(MediaMetadataCompat.METADATA_KEY_TITLE);
            trackNameTextView.setText(title);
        }
    }
}
