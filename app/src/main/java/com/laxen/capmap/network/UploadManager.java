package com.laxen.capmap.network;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by laxen on 6/30/16.
 */
public class UploadManager {
    private final Context context;
    private String putUrl = "";
    private final String twoHyphens = "--";
    private final String lineEnd = "\r\n";
    private final String boundary = "apiclient-" + System.currentTimeMillis();
    private final String mimeType = "multipart/form-data;boundary=" + boundary;
    private double lat;
    private double lon;
    private String location;
    private byte[] multipartBody;
    private String sessionKey;

    private final int BUFFERSIZE = 1024;

    Response.Listener<NetworkResponse> onResponseListener;
    Response.ErrorListener onErrorResponseListener;


    public UploadManager(Context context) {
        this.context = context;
    }

    public void uploadFromUri(Uri uri) {

        Log.d("app", "uploading URI: " + uri.toString());

        // byte array to place file in
        byte[] file = new byte[BUFFERSIZE]; // todo set new length

        // converts file to byte array
        try {
            InputStream stream = context.getContentResolver().openInputStream(uri);
            file = toBytes(stream);
        } catch (IOException e) {
            Log.e("app", "could not convert into byte array. \n");
            Log.e("app", e.getMessage());
        }

        // outputstreams ..
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        DataOutputStream dOut = new DataOutputStream(bOut);

        try {

            // create the different parts of the output stream
            buildParts(dOut, file);

            // pass constructed byte array from outputstream to multipart body
            multipartBody = bOut.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
        }

        // performing put request to server with the URL to put, null as headers, mimeType
        // and constructed multipartBody from output stream
        MultipartRequest multipartRequest = new MultipartRequest(putUrl, null, mimeType,
                multipartBody, onResponseListener, onErrorResponseListener
        );

        RequestHandler.getInstance(context).addToRequestQueue(multipartRequest);
    }

    // converts an input stream into a byte array
    public byte[] toBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFERSIZE];

        int length = 0;

        while ((length = stream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, length);
        }

        return byteBuffer.toByteArray();
    }

    // constructs the actual put request
    private void buildParts(DataOutputStream dOut, byte[] file) throws IOException {

        buildTextPart(dOut, "latitude", lat+"");
        buildTextPart(dOut, "longitude", lon+"");
        buildTextPart(dOut, "location", location);
        buildTextPart(dOut, "session_key", sessionKey);

        // sets filename to lat + ":" + lon + ".mp4" in order to directly find video on server
        // via the lat lon name
        buildFilePart(dOut, file, lat + ":" + lon + ".mp4");

        // send multipart form data necesssary after text and file data
        dOut.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
    }

    // creates the file part of the request
    private void buildFilePart(DataOutputStream dOut, byte[] file, String filename) throws IOException {
        dOut.writeBytes(twoHyphens + boundary + lineEnd);
        dOut.writeBytes("Content-Disposition: form-data; name=\"video\";"
                + " filename=\""  + filename + ".mp4" + "\""
                + lineEnd + "Content-type: video/mp4"
                + lineEnd);
        dOut.writeBytes(lineEnd);

        ByteArrayInputStream fileInputStream = new ByteArrayInputStream(file);
        int bytesAvailable = fileInputStream.available();

        int maxBufferSize = 1024 * 1024;
        int bufferSize = Math.min(bytesAvailable, maxBufferSize);
        byte[] buffer = new byte[bufferSize];

        // read file and write it into form...
        int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

        while (bytesRead > 0) {
            dOut.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        }

        dOut.writeBytes(lineEnd);
    }

    private void buildTextPart(DataOutputStream dataOutputStream, String parameterName, String parameterValue) throws IOException {
        dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + parameterName + "\"" + lineEnd);
        dataOutputStream.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
        dataOutputStream.writeBytes(lineEnd);
        dataOutputStream.writeBytes(parameterValue + lineEnd);
    }

    public void setOnResponseListener(Response.Listener<NetworkResponse> onResponseListener) {
        this.onResponseListener = onResponseListener;
    }

    public void setonErrorResponseListener(Response.ErrorListener onErrorResponseListener) {
        this.onErrorResponseListener = onErrorResponseListener;
    }

    public void setPutUrl(String putUrl) {
        this.putUrl = putUrl;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
