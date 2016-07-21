package com.laxen.capmap;

import android.app.FragmentTransaction;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
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
        ActivityCompat.OnRequestPermissionsResultCallback {


    private boolean debug = true;
    private boolean isSignedIn = false;
    private String sessionKey = "";

    boolean orienChanged = false;
    int lastOrientation = 0;

    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private final int MY_PERMISSIONS_REQUEST_ACCESS_CAMERA = 2;
    private static final int CAMERA_ACCESS_GRANTED = 3;
    private static final int LOCATION_ACCESS_GRANTED = 4;
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
    private static final int RC_SIGN_IN = 300;

    private PermissionHandler permissionHandler;

    // client for handling calls to the google play services api
    private GoogleApiClient apiClient;

    // fragments
    private MapFragmentTab mapFragmentTab;
    private ListFragmentTab listFragmentTab;
    private View signInCard;

    // Util class for storing data on device
    private MediaSaver saver;

    // captured video to be added to map
    private Uri videoUri;

    // fragment for displaying video
    VideoFragment videoFragment = new VideoFragment();

    private GoogleSignInOptions gso;

    // Toolbar toolbar;
    ViewPagerAdapter adapter;
    SlidingTabLayout tabs;
    ViewPager pager;

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

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    permissionHandler.requestCamera();

                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
                }
            });


            saver = new MediaSaver();
        }
    }

    public void initFragments() {
        mapFragmentTab = new MapFragmentTab();
        listFragmentTab = new ListFragmentTab();
        listFragmentTab.subscribe(this);
    }

    public void playVideo(String url) {

        videoUri = Uri.parse(url);
        videoFragment.setVideoUri(Uri.parse(url));

        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.add(R.id.fragmentcontainer, videoFragment).addToBackStack("videoFragment");
        transaction.commit();

    }

    // triggered when a video has caused a screen rotation
    public void playVideoInLandscape() {
        videoFragment.setVideoUri(videoUri);

        FragmentTransaction transaction;

        transaction = this.getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.add(R.id.fragmentcontainer, videoFragment).addToBackStack("videoFragment");
        transaction.commit();
    }

    @Override
    public void onResume() {
        super.onResume();

        //this.fetchData();
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
        //create new Intent
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video image quality to high
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 5); // limits time of video to 5 sec

        if (saver == null) {
            saver = new MediaSaver();
        }

        videoUri = saver.getOutputMediaFileUri(saver.MEDIA_TYPE_VIDEO);

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

                Location location = mapFragmentTab.getLocation();

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

        final UploadManager manager = new UploadManager(this);
        manager.setOnResponseListener(this);
        manager.setonErrorResponseListener(this);
        String putUrl = getString(R.string.server_url_put);
        String putSufix = "";

        // if a sessionkey exists add sufix to request
        if(!sessionKey.equals(""))
            putSufix = "?sessionKey=" + sessionKey;

        manager.setPutUrl(putUrl + putSufix);

        Log.d("app", "uploading to " + putUrl + putSufix + "...");

        try {
            PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                    .getCurrentPlace(apiClient, null);
            result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                @Override
                public void onResult(PlaceLikelihoodBuffer likelyPlaces) {

                    String loc = "";

                    for (PlaceLikelihood place : likelyPlaces) {
                        loc = place.getPlace().getName().toString();
                        break;
                    }

                    Log.d("app", "Setting location to " + loc);
                    Toast.makeText(MainActivity.this, "Location: " + loc, Toast.LENGTH_LONG).show();
                    manager.setLocation(loc);
                    likelyPlaces.release();
                }
            });
        } catch (SecurityException e) {
            Log.e("app", "FAILED WITH PLACES API");
        }

        manager.setLat(lat);
        manager.setLon(lon);

        manager.uploadFromUri(uri);
    }


    @Override
    public void onResponse(Object response) {

        Log.d("app", "Got response " + response.toString());

        if(response.getClass() == JSONObject.class) {
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
            Log.e("app", "MainActivity: " +  error.networkResponse.statusCode + "");
        } catch (NullPointerException e) {
            Log.e("app", "MainActivity: " +  "Critical network error");
            Log.e("app", "MainActivity: " +  error.toString());
        }

        Toast.makeText(MainActivity.this, "Network error :<", Toast.LENGTH_SHORT).show();
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

    private void handleSignInResult(GoogleSignInResult result) {

        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            Toast.makeText(MainActivity.this, "Welcome, " + acct.getEmail(), Toast.LENGTH_SHORT).show();
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

        if(isSignedIn) {
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
                mapFragmentTab.setLocation();
                return;
        }
    }

    public GoogleApiClient getGoogleApiClient() {
        return apiClient;
    }


    // Response from PermissionHandler when popups about permissions are displayed..
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // retry ..
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
}
