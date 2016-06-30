package com.laxen.capmap.network;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;

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
    private byte[] multipartBody;

    private final int BUFFERSIZE = 1024;

    public UploadManager(Context context) {
        this.context = context;
        Log.d("app", "created upload manager");
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
            // building put request
            buildPart(dOut, file, uri.toString());

            // send multipart form data necessary after file data
            dOut.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // pass to multipart body
            multipartBody = bOut.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
        }

        // performing put request to server
        MultipartRequest multipartRequest = new MultipartRequest(putUrl, null, mimeType, multipartBody,

                // on success
                new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                        Toast.makeText(context, "Upload successfully!", Toast.LENGTH_SHORT).show();
                    }
                },

                // on fail
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context, "Upload failed!\r\n" + error.toString(), Toast.LENGTH_SHORT).show();
                        Log.e("app", error.toString() + "");
                    }
                }
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
    private void buildPart(DataOutputStream dOut, byte[] fileData, String fileName) throws IOException {
        dOut.writeBytes(twoHyphens + boundary + lineEnd);
        dOut.writeBytes("Content-Disposition: form-data; name=\"video[video]\"; filename=\""
                + fileName + "\"" + lineEnd + "Content-type: video/mp4" + lineEnd);
        dOut.writeBytes(lineEnd);

        ByteArrayInputStream fileInputStream = new ByteArrayInputStream(fileData);
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

    public void setPutUrl(String putUrl) {
        this.putUrl = putUrl;
    }
}
