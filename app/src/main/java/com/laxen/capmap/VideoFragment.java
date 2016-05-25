package com.laxen.capmap;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;

/**
 * Created by laxen on 5/25/16.
 */
public class VideoFragment extends Fragment {

    private Uri videoUri;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_video, container, false);

        VideoView videoView = (VideoView) view.findViewById(R.id.videoView);
        videoView.setVideoURI(videoUri);

        videoView.start();

        return view;
    }

    public void setVideoUri(Uri videoUri) {
        this.videoUri = videoUri;
    }
}
