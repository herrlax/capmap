package com.laxen.capmap;

import android.Manifest;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.laxen.capmap.network.DownloadManager;
import com.laxen.capmap.network.UploadManager;
import com.laxen.capmap.tabs.ListFragmentTab;
import com.laxen.capmap.tabs.MapFragmentTab;
import com.laxen.capmap.tabs.SlidingTabLayout;
import com.laxen.capmap.utils.PermissionHandler;
import com.laxen.capmap.utils.ViewPagerAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        SurfaceHolder.Callback,
        Response.Listener,
        Response.ErrorListener,
        ListFragmentTab.ListFragmentTabListener,
        View.OnClickListener,
        PermissionHandler.PermissionHandlerListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        LocationListener {


    private boolean debug = true;
    private boolean isSignedIn = false;
    private String sessionKey = "";

    private boolean orienChanged = false;
    private int lastOrientation = 0;

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_CAMERA = 2;
    private static final int CAMERA_ACCESS_GRANTED = 3;
    private static final int LOCATION_ACCESS_GRANTED = 4;
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
    private static final int RC_SIGN_IN = 300;

    private PermissionHandler permissionHandler;

    // client for handling calls to the google play services api
    private GoogleApiClient apiClient;
    private LocationRequest mLocationRequest;
    private String place; // google places place
    private Location mCurrentLocation;

    // fragments
    private MapFragmentTab mapFragmentTab;
    private ListFragmentTab listFragmentTab;
    private View signInCard;
    private View controllers;

    // Util class for storing data on device
    private MediaSaver saver;

    // captured video to be added to map
    //private Uri videoUri;

    // fragment for displaying video
    private VideoFragment videoFragment = new VideoFragment();

    private GoogleSignInOptions gso;

    // Toolbar toolbar;
    private ViewPagerAdapter adapter;
    private SlidingTabLayout tabs;
    private ViewPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // if orienChanged == True, then a wide video has been clicked
        // causing the screen to change orientation -> this is not triggered
        // if the user rotates the screen!
        if (orienChanged) {
            playVideoInLandscape();
        } else {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            getSupportActionBar().hide();

            permissionHandler = PermissionHandler.getInstance(this);
            permissionHandler.subscribe(this);

            initFragments();
            initSlidingTabs();

            initGooglePlayServices();

            // the toolbar of controllers in the bootom of the screen
            controllers = findViewById(R.id.controllers);

            FloatingActionButton cameraButton = (FloatingActionButton) findViewById(R.id.fab);
            cameraButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    permissionHandler.requestCamera();
                    permissionHandler.requestLocation();

                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
                }
            });

            View refreshButton = findViewById(R.id.refresh_button);
            refreshButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    mapFragmentTab.fetchData();
                }
            });

            // hides the search bar..
            hideSearch();

            View searchButton = findViewById(R.id.search_button);
            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    // shows search bar from top and focus it
                    findViewById(R.id.search_tab).animate()
                            .translationY(0);

                    ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

                    // hides toolbar at bottom of screen
                    hideControllers();

                }
            });

            EditText searchField = (EditText) findViewById(R.id.search_field);

            searchField.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {

                    // if the user is done and presses enter, hide search and bring up controls
                    if(keyCode == KeyEvent.KEYCODE_ENTER) {
                        showControllers();
                        hideSearch();
                    }
                    return false;
                }
            });

            searchField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {

                    if(!b) {
                        showControllers();
                        View searchTab = findViewById(R.id.search_tab);
                        searchTab.animate()
                                .translationY(-1000);

                    }
                }
            });


            saver = new MediaSaver();
        }
    }

    public void hideSearch() {
        View searchTab = findViewById(R.id.search_tab);
        searchTab.animate()
                .translationY(-1000);
        searchTab.setVisibility(View.VISIBLE);
    }

    public void hideControllers() {

        controllers.animate()
                .translationY(controllers.getHeight());

    }

    public void showControllers() {

        controllers.animate()
                .translationY(0);

    }

    public void initFragments() {
        mapFragmentTab = new MapFragmentTab();
        listFragmentTab = new ListFragmentTab();
        listFragmentTab.subscribe(this);
    }

    public void playVideos(ArrayList<String> urls, String location) {

        videoFragment.setUris(new ArrayList<Uri>());
        videoFragment.setLocation(location);

        for (String url : urls) {
            videoFragment.getUris().add(Uri.parse(url));
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.add(R.id.fragmentcontainer, videoFragment).addToBackStack("videoFragment");
        transaction.commit();

    }

    // triggered when a video has caused a screen rotation
    public void playVideoInLandscape() {
        Log.e("app", "rotation");

        /*videoFragment.setVideoUri(videoUri);

        FragmentTransaction transaction;

        transaction = this.getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.add(R.id.fragmentcontainer, videoFragment).addToBackStack("videoFragment");
        transaction.commit();*/
    }

    @Override
    public void onResume() {
        super.onResume();

        if (apiClient.isConnected())
            startLocationUpdates();

    }

    public void initGooglePlayServices() {

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(getString(R.string.server_client_id))
                .requestEmail()
                .build();

        // Create an instance of GoogleAPIClient.
        if (apiClient == null) {
            apiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .enableAutoManage(this, this)
                    .build();
        }

    }

    public void startCamera() {
        try {
            PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                    .getCurrentPlace(getGoogleApiClient(), null);
            result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                @Override
                public void onResult(PlaceLikelihoodBuffer likelyPlaces) {

                    for (PlaceLikelihood p : likelyPlaces) {
                        place = p.getPlace().getName().toString();
                        break;
                    }

                    Log.d("app", "Setting location to " + place);
                    Toast.makeText(MainActivity.this, "Location: " + place, Toast.LENGTH_LONG).show();

                    likelyPlaces.release();
                }
            });
        } catch (SecurityException e) {
            Log.e("app", "FAILED WITH PLACES API");
        }

        //create new Intent
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video image quality to high
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 5); // limits time of video to 5 sec

        if (saver == null) {
            saver = new MediaSaver();
        }

        //videoUri = saver.getOutputMediaFileUri(saver.MEDIA_TYPE_VIDEO);

        // start the Video Capture Intent
        startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
    }

    /**
     * Receiving the recorded footage now stored in videoUri
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }


        if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                // refresh current position
                onStart();

                Location location = mCurrentLocation;

                if (location != null) {
                    double lat = Math.floor(location.getLatitude() * 1000) / 1000;
                    double lon = Math.floor(location.getLongitude() * 1000) / 1000;

                    Log.d("app", lat + ":" + lon);

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

        final UploadManager manager = new UploadManager(this);
        manager.setOnResponseListener(this);
        manager.setonErrorResponseListener(this);
        String putUrl = getString(R.string.server_url_put);
        String putSufix = "";

        // if a sessionkey exists add sufix to request
        if (!loadSessionKey().equals(""))
            putSufix = "?sessionKey=" + loadSessionKey();

        manager.setPutUrl(putUrl + putSufix);

        Log.d("app", "uploading to " + putUrl + putSufix + "...");

        manager.setLat(lat);
        manager.setLon(lon);
        manager.setLocation(place);

        Log.d("app", "Location was: " + place);

        manager.uploadFromUri(uri);
    }

    public String loadSessionKey() {
        SharedPreferences sharedPreferences =
                getSharedPreferences(getString(R.string.auth_shared_pref), 0);

        return sharedPreferences.getString(getString(R.string.session_key), "");
    }


    @Override
    public void onResponse(Object response) {

        Log.d("app", "Got response " + response.toString());

        if (response.getClass() == JSONObject.class) {
            try {
                sessionKey = ((JSONObject) response).getString("session_key");

                // saves sessionkey
                writeSharedPref(getString(R.string.auth_shared_pref),
                        getString(R.string.session_key), sessionKey);

                listFragmentTab.fetchData();
            } catch (JSONException e) {
                Log.e("app", "could not handle json object");
            }
        }

        // if response from upload manager
        if (response.getClass() == NetworkResponse.class) {
            Toast.makeText(MainActivity.this, "Upload successful!", Toast.LENGTH_SHORT).show();
            mapFragmentTab.fetchData();
            listFragmentTab.fetchData();
            return;
        }
    }

    public void writeSharedPref(String pref, String key, String value) {
        SharedPreferences sharedPreferences =
                getSharedPreferences(pref, 0);

        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(key, value);
        editor.commit();
    }

    // on fail response from download manager
    @Override
    public void onErrorResponse(VolleyError error) {
        try {
            Log.e("app", "MainActivity: " + error.networkResponse.statusCode + "");
        } catch (NullPointerException e) {
            Log.e("app", "MainActivity: " + "Critical network error");
            Log.e("app", "MainActivity: " + error.toString());
        }

        Toast.makeText(MainActivity.this, "Network error :<", Toast.LENGTH_SHORT).show();
    }

    // when the orientation is changed..
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int orientation = newConfig.orientation;
        if (orientation != lastOrientation) {
            orienChanged = true;
            lastOrientation = orientation;
        }
    }

    // inits the sliding tabs
    public void initSlidingTabs() {

        adapter = new ViewPagerAdapter(getSupportFragmentManager(), 2);
        adapter.setContext(this);
        adapter.setListFragmentTab(listFragmentTab);
        adapter.setMapFragmentTab(mapFragmentTab);

        // sets pager for sliding tabs
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {


            }

            @Override
            public void onPageSelected(int position) {

                // hides and shows controllers depending on tab
                if(position == 0) {
                    showControllers();
                } else {
                    hideSearch();
                    hideControllers();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // tabs for list and map
        tabs = (SlidingTabLayout) findViewById(R.id.tabs);
        tabs.setCustomTabView(R.layout.custom_tab, 0);
        // evens out the space between the tabs
        tabs.setDistributeEvenly(true);
        // Setting the ViewPager For the SlidingTabsLayout
        tabs.setViewPager(pager);

        // colors the scroller
        tabs.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.selector);
            }
        });

        tabs.setVisibility(View.VISIBLE);
    }

    // method for handling click on the sign-in button
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(apiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // callback from the startActivityForResult
    private void handleSignInResult(GoogleSignInResult result) {

        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            Toast.makeText(MainActivity.this, "Welcome, " + acct.getDisplayName(), Toast.LENGTH_SHORT).show();
            isSignedIn = true;
            hideSignIn();

            getSessionKey(acct);
        }
    }

    public void getSessionKey(GoogleSignInAccount acct) {
        DownloadManager manager = new DownloadManager(this);
        manager.setOnResponseListener(this);
        manager.setOnErrorListener(this);
        manager.setGetUrl(getString(R.string.auth_callback) + "?code=" + acct.getServerAuthCode());

        manager.fetchSingleDataObject();
    }

    @Override
    public void onListFragmentTabCreated(View view) {
        SignInButton signInButton = (SignInButton) view.findViewById(R.id.sign_in_button);

        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setScopes(gso.getScopeArray());
        signInButton.setOnClickListener(this);

        signInCard = view.findViewById(R.id.card_view);

        if (isSignedIn) {
            hideSignIn();
        } else {
            showSignIn();
        }
    }

    public void showSignIn() {
        signInCard.setVisibility(View.VISIBLE);
    }

    public void hideSignIn() {
        signInCard.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onPermissionResponse(int responseCode) {

        switch (responseCode) {
            case CAMERA_ACCESS_GRANTED:
                startCamera();
                return;
            case LOCATION_ACCESS_GRANTED:
                if(apiClient != null &&
                        apiClient.isConnected()) {
                    startLocationUpdates(); // if user granted access to device's location
                }

                return;
        }
    }

    public GoogleApiClient getGoogleApiClient() {
        return apiClient;
    }


    // Response from PermissionHandler when popups about permissions are displayed..
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {


                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // retry to get that location..
                    permissionHandler.requestLocation();

                } else {
                    Toast.makeText(MainActivity.this,
                            "Location access is needed to display your location",
                            Toast.LENGTH_LONG).show();

                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_ACCESS_CAMERA: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    permissionHandler.requestCamera();

                } else {

                    Toast.makeText(MainActivity.this,
                            "Camera access is needed to use this functionality",
                            Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
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
        startLocationUpdates(); // try getting that location
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopLocationUpdates();
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // if permission has not been granted, request it..
            permissionHandler.requestLocation();
            return;
        }

        if(mLocationRequest == null)
            mLocationRequest = LocationRequest.create();

        LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, mLocationRequest, this);
    }
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                apiClient, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mapFragmentTab.setLocation(mCurrentLocation);
    }

    /**
     * Response from onStop() or whenever connection is closed
     */
    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        if (debug) {
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
    public void onDestroy() {

        mapFragmentTab.die();
        videoFragment.die();

        apiClient.disconnect();

        onStop();

        super.onDestroy();
    }
}
