package com.laxen.capmap;

import android.Manifest;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.laxen.capmap.network.RequestHandler;
import com.laxen.capmap.utils.VideoItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleMap.OnMarkerClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SurfaceHolder.Callback {


    private boolean debug = true;


    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private final int MY_PERMISSIONS_REQUEST_ACCESS_CAMERA = 2;
    private final int MY_PERMISSIONS_REQUEST_ACCESS_READ_EXTERNAL_STORAGE = 3;
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;

    // client for handling calls to the google play services api
    private GoogleApiClient apiClient;

    // location of the device
    private Location location;

    // last clicked location
    private String locationString;

    // google maps
    private GoogleMap map;
    private MapFragment mMapFragment;

    // Util class for storing data on device
    private MediaSaver saver;

    // captured video to be added to map
    private Uri videoUri;

    // Maps with all uris and latlongs
    private Map<String, Uri> uriMap;

    // fragment for displaying video
    VideoFragment videoFragment = new VideoFragment();

    private final String PREFS_NAME = "SAVED_VIDEOS";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initGooglePlayServices();

        createMap();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestCamera();

                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
            }
        });

        saver = new MediaSaver();
        uriMap = new HashMap<>();
    }

    public void initGooglePlayServices() {
        // Create an instance of GoogleAPIClient.
        if (apiClient == null) {
            apiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }


    public void createMap() {

        mMapFragment = MapFragment.newInstance();

        FragmentTransaction fragmentTransaction =
                getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.container, mMapFragment);
        fragmentTransaction.commit();

        mMapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("app", "map ready");
        map = googleMap;
        // mapHandler = new MapHandler(map, this, jobsModel);

        map.setOnMyLocationButtonClickListener(this);
        map.setOnMarkerClickListener(this);

        // map.setOnMapClickListener(mapHandler);
        // map.setOnMapLongClickListener(mapHandler);

        // for testing
        if (debug) {
            LatLng SWEDEN = new LatLng(62.2315, 16.1932);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(SWEDEN, 4.5f));
        }

        loadLocalData();
        fetchData();

    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void requestCamera() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {

                // if the user has denied the permission before

                // request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_ACCESS_CAMERA);

            } else {

                // request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_ACCESS_CAMERA);

            }

        } else {
            startCamera();
        }
    }

    public void startCamera() {
        //create new Intent
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video image quality to high

        if(saver == null ) {
            saver = new MediaSaver();
        }

        videoUri = saver.getOutputMediaFileUri(saver.MEDIA_TYPE_VIDEO);

        // start the Video Capture Intent
        startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }


    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void requestLocation() {

        Log.e("app", "requesting location");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // if the user has denied the permission before

                // request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

            } else {

                // request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

            }

        } else {

            // sets location
            location = LocationServices.FusedLocationApi.getLastLocation(apiClient);
        }
    }

    /**
     * Enables the writing of external storage
     */
    private void requestRead() {

        Log.e("app", "requesting location");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // if the user has denied the permission before

                // request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_ACCESS_READ_EXTERNAL_STORAGE);

            } else {

                // request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_ACCESS_READ_EXTERNAL_STORAGE);

            }

        } else {

            // if permission is granted
            Uri uri = uriMap.get(locationString);

            if(uri == null) {

                if (debug) {
                    Toast.makeText(MainActivity.this, "Nothing's there :o", Toast.LENGTH_SHORT).show();
                }

            } else {

                videoFragment.setVideoUri(uri);

                FragmentTransaction transaction;

                transaction = this.getFragmentManager().beginTransaction();
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.add(R.id.container, videoFragment).addToBackStack("videoFragment");
                transaction.commit();

            }
        }
    }


    // Handles all permissions
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // retry ..
                    requestLocation();

                } else {

                    // permission denied
                    Toast.makeText(MainActivity.this, "Location is needed to post videos :<", Toast.LENGTH_SHORT).show();

                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_ACCESS_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    startCamera();

                } else {

                    // permission denied
                    // permission granted
                    Toast.makeText(MainActivity.this, "Function requires camera", Toast.LENGTH_SHORT).show();

                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_ACCESS_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    requestRead();

                } else {

                    // permission denied
                    // permission granted
                    Toast.makeText(MainActivity.this, "Function requires access to external storage", Toast.LENGTH_LONG).show();

                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /**
     * Receiving the recorded footage now stored in videoUri
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {


                // refresh current position
                onStart();

                if (location != null) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();

                    if(uriMap == null) {
                        uriMap = new HashMap<>();
                    }

                    // stores the video to map
                    uriMap.put(lat+ ";" + lon, data.getData());
                    saveLocally(lat+ ";" + lon + ";" + data.getData().toString());

                    map.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title(lat+ ";" + lon));
                }

            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the video capture
            } else {
                Toast.makeText(MainActivity.this, "Video capture failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void saveLocally(String latlongVid){

        // saves coordinates and file pointers locally
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        // retrieves locally stored data
        Set<String> savedStrings = settings.getStringSet("videos", new HashSet<String>());


        Set<String> newStrings = new HashSet<>();

        // adds the saved data to a new string set
        for(String str : savedStrings) {
            newStrings.add(str);
        }

        // finally adds the new one to the string set
        newStrings.add(latlongVid);


        Log.d("app", "stored : " + newStrings.toString());
        editor.putStringSet("videos", newStrings);
        editor.commit();

    }


    // fetches data from server
    public void fetchData() {

        String url = "http://jsonplaceholder.typicode.com/todos";

        JsonArrayRequest jsObjRequest = new JsonArrayRequest
            (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {

                @Override
                public void onResponse(JSONArray response) {

                    Log.d("app", response.toString());

                    JSONObject json1 = new JSONObject();
                    JSONObject json2 = new JSONObject();
                    JSONObject json3 = new JSONObject();

                    try {
                        json1.put("lat", "51.5074");
                        json1.put("lon", "0.1278");

                        json2.put("lat", "50.8225");
                        json2.put("lon", "0.1372");

                        json3.put("lat", "39.9607");
                        json3.put("lon", "75.6055");

                    } catch (JSONException e) {

                    }

                    JSONArray array = new JSONArray();

                    array.put(json1);
                    array.put(json2);
                    array.put(json3);

                    // adds the dummy data to map
                    for(VideoItem item : jsonArrayToObjects(array)){
                        Log.d("app", item.toString());
                    }

                    // adds the dummy data to map
                    addToMap(jsonArrayToObjects(array));

                    // adds the fetched data to map
                    /*for(VideoItem item : jsonArrayToObjects(response)){
                        Log.d("app", item.toString());
                    }*/
                }

            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(MainActivity.this, "Volley didn't work", Toast.LENGTH_SHORT).show();

                }
            });


        // Access the RequestQueue through your singleton class.
        RequestHandler.getInstance(this).addToRequestQueue(jsObjRequest);
    }

    public Set<VideoItem> jsonArrayToObjects(JSONArray jsonArray) {

        Set<VideoItem> set = new HashSet<>();
        Gson gson = new Gson();


        for (int i=0; i<jsonArray.length(); i++) {

            try{
                VideoItem videoItem = gson.fromJson(jsonArray.get(i).toString(), VideoItem.class);
                set.add(videoItem);
            } catch (JSONException e) {
                Log.e("app", e.getMessage());
            }

        }

        return set;
    }

    // adds a set of video items to the map as markers
    public void addToMap(Set<VideoItem> items) {

        for(VideoItem item : items) {

            // todo add to urimap

            map.addMarker(new MarkerOptions()
                    .position(new LatLng(item.getLat(), item.getLon()))
                    .title(item.getLat()+ ";" + item.getLon()));
        }
    }

    // todo remove this method
    public void addMarkersToMap(Set<String> data) {

        for(String str : data) {
            Log.d("app", str);

            /*String[] strs = str.split(";");

            if(strs.length == 3) {

                double lat = Double.parseDouble(strs[0]);
                double lon = Double.parseDouble(strs[1]);
                Uri uri = Uri.parse(strs[2]);

                uriMap.put(lat+ ";" + lon, uri);

                map.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title(lat+ ";" + lon));
            }*/
        }
    }

    // loads local data and populates map
    public void loadLocalData() {

        // saves coordinates and file pointers locally
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        Set<String> savedStrings = settings.getStringSet("videos", new HashSet<String>());

        // adds local data to map
        addMarkersToMap(savedStrings);
    }




    //  map.setMyLocationEnabled(true);

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        // gets the locationString of the marker (i.e the title)
        locationString = marker.getTitle();


        // gets uri from location
        requestRead();

        return false;
    }


    /**
     * Starts the connection to google play services
     */
    protected void onStart() {
        Log.e("app", "onStart()");
        apiClient.connect();
        super.onStart();
    }

    /**
     * Stops the connection to google play services
     */
    protected void onStop() {
        apiClient.disconnect();
        super.onStop();
    }


    /**
     * Response from onStart()
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Log.e("app", "connected: ");

        // gets the last known position for the device
        requestLocation();

    }

    /**
     * Response from onStop() or whenever connection is closed
     */
    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        if(debug) {
            Toast.makeText(MainActivity.this, "Connection refused :<", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
