package com.laxen.capmap;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import java.util.ArrayList;

/**
 * Created by laxen on 5/25/16.
 */
public class VideoFragment extends Fragment {

    private VideoView videoView;
    private ArrayList<Uri> uris;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_video, container, false);

        final RelativeLayout videoBack = (RelativeLayout) view.findViewById(R.id.videoBack);
        videoView = (VideoView) view.findViewById(R.id.videoView);

        videoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        });


        Log.d("app", "ALL URIS:");
        Log.d("app", uris.toString());

        videoView.setVideoURI(uris.remove(0));
        videoView.requestFocus();

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            // Close the progress bar and play the video
            public void onPrepared(MediaPlayer mediaPlayer) {

                if(mediaPlayer.getVideoWidth() > mediaPlayer.getVideoHeight()) {
                    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }

                videoView.start();
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playNext();
            }
        });

        return view;
    }

    public void playNext() {
        Log.d("app", "URIS:");
        Log.d("app", uris.toString());
        if(!uris.isEmpty()) {
            videoView.setVideoURI(uris.remove(0));
        } else {

            // end fragment if no video is left to play
            videoView.stopPlayback();
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            getActivity().getFragmentManager().beginTransaction().remove(VideoFragment.this).commit();
        }
    }

    public void setUris(ArrayList<Uri> uris) {
        this.uris = uris;
    }

    public ArrayList<Uri> getUris() {
        return uris;
    }

    public void die() {
        if(videoView != null && videoView.isPlaying()) {
            videoView.stopPlayback();
        }
    }
}
