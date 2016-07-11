package com.laxen.capmap.tabs;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.laxen.capmap.R;
import com.laxen.capmap.utils.VideoItem;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by laxen on 7/10/16.
 */
public class ListFragmentTab extends Fragment {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_myvideos,container,false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true); // for improved performance

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        List<VideoItem> dummyData = new LinkedList<>();
        dummyData.add(new VideoItem("23.4", "32.2", ""));
        dummyData.add(new VideoItem("64.4", "86.2", ""));
        dummyData.add(new VideoItem("27.4", "12.2", ""));

        mAdapter = new ListFragmentAdapter(dummyData, getContext());
        mRecyclerView.setAdapter(mAdapter);


        return view;
    }

}
