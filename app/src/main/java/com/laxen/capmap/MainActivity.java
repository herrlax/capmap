package com.laxen.capmap;

import android.Manifest;
import android.app.FragmentTransaction;
import android.content.Context;
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
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
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
import com.laxen.capmap.network.RequestHandler;
import com.laxen.capmap.utils.JsonHelper;
import com.laxen.capmap.utils.MultipartRequest;
import com.laxen.capmap.utils.VideoItem;

import org.json.JSONArray;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleMap.OnMarkerClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SurfaceHolder.Callback {


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
    private Map<String, String> urlMap;

    // fragment for displaying video
    VideoFragment videoFragment = new VideoFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if(orienChanged) {
            Log.d("app", "YEA");
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
            urlMap = new HashMap<>();
        }
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
        map.setOnMyLocationButtonClickListener(this);
        map.setOnMarkerClickListener(this);
        requestLocation();


        // for testing
        if (debug) {
            LatLng SWEDEN = new LatLng(62.2315, 16.1932);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(SWEDEN, 4.5f));
        }

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

                    uploadVideo(data.getData());

                    map.addMarker(new MarkerOptions()
                            .position(new LatLng(lat, lon))
                            .title(lat+ ";" + lon));
                }

            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the video capture
            } else {
                Toast.makeText(MainActivity.this, "Video capture failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final Context context = this;
    private final String twoHyphens = "--";
    private final String lineEnd = "\r\n";
    private final String boundary = "apiclient-" + System.currentTimeMillis();
    private final String mimeType = "multipart/form-data;boundary=" + boundary;
    private byte[] multipartBody;

    public void uploadVideo(Uri uri) {

        String url = "http://10.1.0.4:3000/videos";
        Log.d("app", uri.toString());

        byte[] videoBytes = new byte[200];

        try {
            InputStream iStream = getContentResolver().openInputStream(uri);
            videoBytes = getBytes(iStream);
        } catch (Exception e) {
            Log.e("app", "could not convert into byte array");
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        try {

            // the first file
            buildPart(dos, videoBytes, uri.toString());

            // send multipart form data necessary after file data
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            // pass to multipart body
            multipartBody = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        MultipartRequest multipartRequest = new MultipartRequest(url, null, mimeType,
                multipartBody, new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response) {
                Toast.makeText(context, "Upload successfully!", Toast.LENGTH_SHORT).show();
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context, "Upload failed!\r\n" + error.toString(), Toast.LENGTH_SHORT).show();
                Log.e("app", error.toString() + "");
            }
        });

        RequestHandler.getInstance(context).addToRequestQueue(multipartRequest);
    }

    private void buildPart(DataOutputStream dataOutputStream, byte[] fileData, String fileName) throws IOException {
        dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"video[video]\"; filename=\""
                + fileName + "\"" + lineEnd + "Content-type: video/mp4" + lineEnd);
        dataOutputStream.writeBytes(lineEnd);

        ByteArrayInputStream fileInputStream = new ByteArrayInputStream(fileData);
        int bytesAvailable = fileInputStream.available();

        int maxBufferSize = 1024 * 1024;
        int bufferSize = Math.min(bytesAvailable, maxBufferSize);
        byte[] buffer = new byte[bufferSize];

        // read file and write it into form...
        int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

        while (bytesRead > 0) {
            dataOutputStream.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        }

        dataOutputStream.writeBytes(lineEnd);
    }

    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    // fetches data from server
    public void fetchData() {

        String url = "http://malmqvist.it/api";

        JsonArrayRequest jsObjRequest = new JsonArrayRequest
            (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {

                @Override
                public void onResponse(JSONArray response) {

                    Log.d("app", response.toString());

                    // adds the dummy data to map
                    addToMap(JsonHelper.jsonArrayToSet(response));

                    // adds the fetched data to map
                    for(VideoItem item : JsonHelper.jsonArrayToSet(response)){
                        Log.d("app", item.toString());
                    }
                }

            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("app", error.getMessage());
                    Toast.makeText(MainActivity.this, "Network error :<", Toast.LENGTH_SHORT).show();

                }
            });


        // Access the RequestQueue through your singleton class.
        RequestHandler.getInstance(this).addToRequestQueue(jsObjRequest);
    }


    // adds a set of video items to the map as markers
    public void addToMap(Set<VideoItem> items) {

        if(urlMap == null) {
            urlMap = new HashMap<>();
        }

        urlMap.clear();

        for(VideoItem item : items) {

            String key = item.getLat()+ ";" + item.getLon();

            // todo add to urimap
            urlMap.put(key, item.getVideoUrl());

            map.addMarker(new MarkerOptions()
                    .position(new LatLng(item.getLat(), item.getLon()))
                    .title(key));
        }
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

        // gets url to video from location
        String url = urlMap.get(locationString);

        if(url == null || url.equals("")) {

            if (debug) {
                Toast.makeText(MainActivity.this, "Nothing's there :o", Toast.LENGTH_SHORT).show();
            }

        } else {
            videoUri = Uri.parse(url);

            videoFragment.setVideoUri(Uri.parse(url));

            FragmentTransaction transaction;

            transaction = this.getFragmentManager().beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.add(R.id.container, videoFragment).addToBackStack("videoFragment");
            transaction.commit();

        }

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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.d("app", "CHANGE");
        int orientation = newConfig.orientation;
        if(orientation != lastOrientation){
            orienChanged  = true;
            lastOrientation = orientation ;
        }

    }
}
