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

/**
 * Created by laxen on 5/25/16.
 */
public class VideoFragment extends Fragment {

    private Uri videoUri;
    private VideoView videoView;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_video, container, false);

        final RelativeLayout videoBack = (RelativeLayout) view.findViewById(R.id.videoBack);
        videoView = (VideoView) view.findViewById(R.id.videoView);

        videoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // end fragment when user clicks video
                videoView.stopPlayback();
                getActivity().getFragmentManager().beginTransaction().remove(VideoFragment.this).commit();
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                Log.e("App", "ending fragment");
            }
        });

        videoView.setVideoURI(videoUri);
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

        return view;
    }

    public void setVideoUri(Uri videoUri) {
        this.videoUri = videoUri;
    }
}
