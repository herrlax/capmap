package com.laxen.capmap.tabs;

import android.os.Bundle;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.laxen.capmap.R;

import java.util.ArrayList;

public class MapFragmentTab extends Fragment {


    public interface MapFragmentTabListener {
        void onMapFragmentCreated();
    }

    ArrayList<MapFragmentTabListener> subs;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        if(subs == null) {
            subs = new ArrayList<>();
        }

        for (MapFragmentTabListener listener : subs) {
            listener.onMapFragmentCreated();
        }


        return view;
    }

    public void subscribe(MapFragmentTabListener listener) {
        if (subs == null) {
            subs = new ArrayList<>();
        }

        subs.add(listener);
    }

}