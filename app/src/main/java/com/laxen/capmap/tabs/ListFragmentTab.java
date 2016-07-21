package com.laxen.capmap.tabs;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Response;
import com.laxen.capmap.MainActivity;
import com.laxen.capmap.R;
import com.laxen.capmap.network.DownloadManager;
import com.laxen.capmap.utils.JsonHelper;
import com.laxen.capmap.utils.VideoItem;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created by laxen on 7/10/16.
 */
public class ListFragmentTab extends Fragment implements Response.Listener<JSONArray> {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private View signInCard;

    private MainActivity activity;

    public interface ListFragmentTabListener {
        void onListFragmentTabCreated(View view);
    }

    ArrayList<ListFragmentTabListener> subs;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_myvideos,container,false);


        // if a local sessionkey is found, check if it's active with server
        if(loadSessionKey().equals("")) {
            signInCard = view.findViewById(R.id.card_view);
            signInCard.setVisibility(View.INVISIBLE);
        }

        activity = (MainActivity) this.getActivity();

        mRecyclerView = (RecyclerView) view.findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true); // for improved performance

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        if(subs != null) {
            for (ListFragmentTabListener sub : subs) {
                sub.onListFragmentTabCreated(view);
            }
        }

        fetchData();
        return view;
    }

    public void subscribe(ListFragmentTabListener listener) {
        if (subs == null) {
            subs = new ArrayList<>();
        }

        subs.add(listener);
    }

    // fetches data from server
    public void fetchData() {

        if(loadSessionKey().equals("")){
            Log.e("app", "no session key stored locally");
            return;
        }

        Log.d("app", "loaded sess.key locally: " + loadSessionKey());

        DownloadManager manager = new DownloadManager(activity);
        manager.setOnResponseListener(this);
        manager.setOnErrorListener(activity);
        manager.setGetUrl(getString(R.string.server_url_get) + "?sessionKey=" + loadSessionKey());
        manager.fetchData();
    }

    public String loadSessionKey() {
        SharedPreferences sharedPreferences =
                getActivity().getSharedPreferences(getString(R.string.auth_shared_pref), 0);

        return sharedPreferences.getString(getString(R.string.session_key), "");
    }

    // response from fetching data..
    @Override
    public void onResponse(JSONArray response) {

        // hides sign in card if a successful response is received
        signInCard.setVisibility(View.INVISIBLE);

        // if response from download manager
        if(response.getClass() == JSONArray.class) {
            addToList(JsonHelper.jsonArrayToSet(response));
            return;
        }
    }

    // adds a set of video items to the map as markers
    public void addToList(Set<VideoItem> items) {

        mAdapter = new ListFragmentAdapter(items.toArray(), activity);
        mRecyclerView.setAdapter(mAdapter);
    }

}
