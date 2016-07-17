package com.laxen.capmap.tabs;

import android.app.FragmentTransaction;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Response;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.laxen.capmap.MainActivity;
import com.laxen.capmap.R;
import com.laxen.capmap.network.DownloadManager;
import com.laxen.capmap.utils.JsonHelper;
import com.laxen.capmap.utils.PermissionHandler;
import com.laxen.capmap.utils.VideoItem;

import org.json.JSONArray;

import java.util.Set;

public class MapFragmentTab extends Fragment
        implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        Response.Listener<JSONArray> {

    // google maps
    private GoogleMap map;
    private MapFragment mMapFragment;

    // location of the device
    private Location location;

    private MainActivity activity;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        this.activity = (MainActivity) getActivity();
        initMaps();

        return view;
    }

    public void initMaps() {
        mMapFragment = MapFragment.newInstance();

        FragmentTransaction fragmentTransaction =
                activity.getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.container, mMapFragment);
        fragmentTransaction.commit();

        mMapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setOnMarkerClickListener(this);
        PermissionHandler.getInstance(activity).requestLocation();

        fetchData();
    }

    // fetches data from server
    public void fetchData() {
        DownloadManager manager = new DownloadManager(activity);
        manager.setOnResponseListener(this);
        manager.setOnErrorListener(activity);
        manager.setGetUrl(getString(R.string.server_url_get));
        manager.fetchData();
    }

    @Override
    public void onResponse(JSONArray response) {
        // if response from download manager
        if(response.getClass() == JSONArray.class) {
            addToMap(JsonHelper.jsonArrayToSet(response));
            return;
        }
    }

    // adds a set of video items to the map as markers
    public void addToMap(Set<VideoItem> items) {

        for(VideoItem item : items) {
            map.addMarker(new MarkerOptions()
                    .position(new LatLng(Double.parseDouble(item.getLatitude()), Double.parseDouble(item.getLongitude())))
                    .title(item.getUrl())); // sets video url as title for playback
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        activity.playVideo(marker.getTitle());

        return true;
    }

    public void setLocation() {

        Log.d("app", "setting location..");

        try {
            location = LocationServices.FusedLocationApi.getLastLocation(activity.getGoogleApiClient());
            // zooms in on current location
            if(location != null) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
                map.animateCamera(cameraUpdate);
                map.setMyLocationEnabled(true);
            }

        } catch (SecurityException e) {
            Log.e("app", "error " + e.toString());
        }

    }

    public Location getLocation() {
        return location;
    }

}