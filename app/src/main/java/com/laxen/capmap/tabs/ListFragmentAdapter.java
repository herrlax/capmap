package com.laxen.capmap.tabs;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.laxen.capmap.MainActivity;
import com.laxen.capmap.R;
import com.laxen.capmap.utils.VideoItem;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;


/**
 * Created by laxen on 7/11/16.
 */
public class ListFragmentAdapter extends RecyclerView.Adapter<ListFragmentAdapter.ViewHolder> {

    private Object[] videos;
    private MainActivity context;

    // Provide a suitable constructor (depends on the kind of dataset)
    public ListFragmentAdapter(Object[] videos, MainActivity context) {
        this.context = context;
        this.videos = videos;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_card, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final VideoItem videoItem = (VideoItem) videos[position];

        Log.d("app", videoItem.toString());

        holder.playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> urls = new ArrayList<String>();
                urls.add(videoItem.getUrl());
                context.playVideos(urls);
            }
        });

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // todo remove video
            }
        });

        holder.timeStampTextView.setText("Video is " + videoItem.getTimestamp());
        holder.expiresTextView.setText("Expires in " + videoItem.getExpires());

        // setting thumbnail ..
        Picasso.with(context)
                .load(videoItem.getThumbnail())
                .fit()
                .centerCrop()
                .into(holder.cardBackground);


        if(!videoItem.getLongitude().equals("")) {
            holder.locationTextView.setText(videoItem.getLocation());
        } else {
            holder.locationTextView.setText("Lat: " + videoItem.getLatitude()
                + ", Long: " + videoItem.getLongitude());
        }


    }

    @Override
    public int getItemCount() {
        return videos.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        protected TextView timeStampTextView;
        protected TextView locationTextView;
        protected TextView viewsTextView;
        protected TextView deleteButton;
        protected TextView playButton;
        protected TextView expiresTextView;
        protected CardView cardView;
        protected ImageView cardBackground;

        public View view;
        public ViewHolder(View v) {
            super(v);
            timeStampTextView = (TextView) v.findViewById(R.id.timeStampTextView);
            locationTextView = (TextView) v.findViewById(R.id.locationTextView);
            viewsTextView = (TextView) v.findViewById(R.id.viewsTextView);
            expiresTextView = (TextView) v.findViewById(R.id.expiresText);
            deleteButton = (TextView) v.findViewById(R.id.deleteButton);
            playButton = (TextView) v.findViewById(R.id.playButton);
            cardView = (CardView) v.findViewById(R.id.card_view);
            cardBackground = (ImageView)v .findViewById(R.id.cardBackground);

        }
    }

}
