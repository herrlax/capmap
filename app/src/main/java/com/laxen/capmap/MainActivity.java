package com.laxen.capmap;

import android.Manifest;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.laxen.capmap.network.DownloadManager;
import com.laxen.capmap.network.UploadManager;
import com.laxen.capmap.utils.JsonHelper;
import com.laxen.capmap.utils.VideoItem;

import org.json.JSONArray;

import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        SurfaceHolder.Callback,
        Response.Listener,
        Response.ErrorListener{


    private boolean debug = true;

    boolean orienChanged = false;
    int lastOrientation = 0;

    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private final int MY_PERMISSIONS_REQUEST_ACCESS_CAMERA = 2;
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;

    // client for handling calls to the google play services api
    private GoogleApiClient apiClient;

    // location of the device
    private Location location;

    // google maps
    private GoogleMap map;
    private MapFragment mMapFragment;

    // Util class for storing data on device
    private MediaSaver saver;

    // captured video to be added to map
    private Uri videoUri;

    // fragment for displaying video
    VideoFragment videoFragment = new VideoFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if(orienChanged) {

            videoFragment.setVideoUri(videoUri);

            FragmentTransaction transaction;

            transaction = this.getFragmentManager().beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.add(R.id.container, videoFragment).addToBackStack("videoFragment");
            transaction.commit();
        } else {
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
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        this.fetchData();
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

        map = googleMap;
        map.setOnMarkerClickListener(this);
        requestLocation();

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
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 5); // limits time of video to 5 sec

        if(saver == null ) {
            saver = new MediaSaver();
        }

        videoUri = saver.getOutputMediaFileUri(saver.MEDIA_TYPE_VIDEO);

        // start the Video Capture Intent
        startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
    }



    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void requestLocation() {

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

            // zooms in on current location
            if(location != null) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
                map.animateCamera(cameraUpdate);
                map.setMyLocationEnabled(true);
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
                    Toast.makeText(MainActivity.this, "Location is needed to post videos :<",
                            Toast.LENGTH_SHORT).show();

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
                    Toast.makeText(MainActivity.this, "Function requires camera",
                            Toast.LENGTH_SHORT).show();

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

                    // uploads video to server
                    uploadVideo(data.getData(), lat, lon);
                }

            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the video capture
            } else {
                Toast.makeText(MainActivity.this, "Video capture failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // uploads a video file through the UploadManager
    public void uploadVideo(Uri uri, double lat, double lon) {

        UploadManager manager = new UploadManager(this);
        manager.setOnResponseListener(this);
        manager.setonErrorResponseListener(this);
        manager.setPutUrl(getString(R.string.server_url_put));
        manager.setLat(lat);
        manager.setLon(lon);

        manager.uploadFromUri(uri);
    }


    // fetches data from server
    public void fetchData() {
        DownloadManager manager = new DownloadManager(this);
        manager.setOnResponseListener(this);
        manager.setOnErrorListener(this);
        manager.setGetUrl(getString(R.string.server_url_get));
        manager.fetchData();
    }

    @Override
    public void onResponse(Object response) {

        // if response from download manager
        if(response.getClass() == JSONArray.class) {
            addToMap(JsonHelper.jsonArrayToSet((JSONArray) response));
            return;
        }

        // if response from upload manager
        if(response.getClass() == NetworkResponse.class) {
            Toast.makeText(MainActivity.this, "Upload successful!", Toast.LENGTH_SHORT).show();
            fetchData();
            return;
        }

    }

    // on fail response from download manager
    @Override
    public void onErrorResponse(VolleyError error) {
        Log.e("app", error.getMessage()+"");
        Toast.makeText(MainActivity.this, "Network error :<", Toast.LENGTH_SHORT).show();
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

        // the video url is bound to the title
        String url = marker.getTitle();

        videoUri = Uri.parse(url);
        videoFragment.setVideoUri(Uri.parse(url));

        FragmentTransaction transaction = this.getFragmentManager().beginTransaction();
        
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.add(R.id.container, videoFragment).addToBackStack("videoFragment");
        transaction.commit();

        return true;
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
     * This is run when the app is closed / hidden
     */
    protected void onStop() {
        Log.w("app", "onStop()");
        apiClient.disconnect();
        super.onStop();
    }


    /**
     * Response from onStart()
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
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

    /**
     * Memory optimization
     */
    @Override
    public void onTrimMemory (int level) {
        super.onTrimMemory(level);

        Log.d("app", level+ "");

        // when app's UI is hidden
        if(level == TRIM_MEMORY_UI_HIDDEN) {
            map.stopAnimation(); // stop animation of map

        }

    }

    // when the orientation is changed..
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int orientation = newConfig.orientation;
        if(orientation != lastOrientation){
            orienChanged  = true;
            lastOrientation = orientation ;
        }
    }
}
