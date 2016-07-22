package com.laxen.capmap.utils;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.laxen.capmap.MainActivity;

import java.util.ArrayList;

/**
 * Created by laxen on 7/17/16.
 */
public class PermissionHandler {

    public interface PermissionHandlerListener {
        void onPermissionResponse(int responseCode);
    }

    private ArrayList<PermissionHandlerListener> listeners = new ArrayList<>();

    private static PermissionHandler instance;
    private static MainActivity context;

    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private final int MY_PERMISSIONS_REQUEST_ACCESS_CAMERA = 2;
    private static final int CAMERA_ACCESS_GRANTED = 3;
    private static final int LOCATION_ACCESS_GRANTED = 4;

    private PermissionHandler(final MainActivity context) {
        this.context = context;
    }

    public static synchronized PermissionHandler getInstance(MainActivity context) {
        if (instance == null)
            instance = new PermissionHandler(context);

        return instance;
    }

    public void subscribe(PermissionHandlerListener listener) {
        listeners.add(listener);
    }

    public void requestCamera() {

        // if permission is not granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(context,
                    Manifest.permission.CAMERA)) {

                // request the permission from user, response callback in onRequestPermissionsResult
                ActivityCompat.requestPermissions(context,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_ACCESS_CAMERA);

            } else {

                // request the permission
                ActivityCompat.requestPermissions(context,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_ACCESS_CAMERA);

            }

        } else {
            for (PermissionHandlerListener listener: listeners) {
                listener.onPermissionResponse(CAMERA_ACCESS_GRANTED);
            }
        }
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    public void requestLocation() {

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(context,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // if the user has denied the permission before

                // request the permission
                ActivityCompat.requestPermissions(context,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

            } else {

                // request the permission
                ActivityCompat.requestPermissions(context,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }

        } else { // if all is good

            for (PermissionHandlerListener listener: listeners) {
                listener.onPermissionResponse(LOCATION_ACCESS_GRANTED);
            }

        }
    }
}
