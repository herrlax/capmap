package com.laxen.capmap.tabs;

import android.content.Context;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.laxen.capmap.R;
import com.laxen.capmap.utils.VideoItem;

import java.util.List;

/**
 * Created by laxen on 7/11/16.
 */
public class ListFragmentAdapter extends RecyclerView.Adapter<ListFragmentAdapter.ViewHolder> {

    private List<VideoItem> videos;
    private Context context;

    // Provide a suitable constructor (depends on the kind of dataset)
    public ListFragmentAdapter(List<VideoItem> videos, Context context) {
        this.context = context;
        this.videos = videos;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_card, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final VideoItem videoItem = videos.get(position);

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // todo display video from videoItem
            }
        });

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // todo remove video
            }
        });

        holder.timeStampTextView.setText("2016-04-02 10:33");
        holder.locationTextView.setText("Sierra Nevada, CA, United States");

    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        protected TextView timeStampTextView;
        protected TextView locationTextView;
        protected TextView viewsTextView;
        protected ImageButton deleteButton;
        protected CardView cardView;
        protected View cardBackground;

        public View view;
        public ViewHolder(View v) {
            super(v);
            timeStampTextView = (TextView) v.findViewById(R.id.timeStampTextView);
            locationTextView = (TextView) v.findViewById(R.id.locationTextView);
            viewsTextView = (TextView) v.findViewById(R.id.viewsTextView);
            deleteButton = (ImageButton) v.findViewById(R.id.deleteButton);
            cardView = (CardView) v.findViewById(R.id.card_view);
            cardBackground = v.findViewById(R.id.cardBackground);

        }
    }
}
