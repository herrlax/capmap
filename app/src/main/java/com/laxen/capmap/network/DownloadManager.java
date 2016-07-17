package com.laxen.capmap.network;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by laxen on 6/30/16.
 */
public class DownloadManager {
    private final Context context;
    private String getUrl = "";
    private Response.Listener responseListener;
    private Response.ErrorListener errorListener;
    private String serverAuthCode = "";

    public DownloadManager(Context context) {
        this.context = context;
    }

    public void setOnResponseListener(Response.Listener<JSONArray> responseListener) {
        this.responseListener = responseListener;
    }

    public void setOnErrorListener(Response.ErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    public void fetchData() {

        JsonRequest request;
        Log.d("app", "serverAuthCode: " + serverAuthCode);

        if(!serverAuthCode.equals("")) {

            request = new JsonObjectRequest(
                    Request.Method.GET, getUrl + "?code=" + serverAuthCode, null, responseListener, errorListener);

            Log.d("app", "actual request: " + request.toString());

        } else {

            request = new JsonArrayRequest(
                    Request.Method.GET, getUrl, null, responseListener, errorListener);
        }


        // Access the RequestQueue through your singleton class.
        RequestHandler.getInstance(context).addToRequestQueue(request);
    }

    public void setGetUrl(String getUrl) {
        this.getUrl = getUrl;
    }

    public void setServerAuthCode(String serverAuthCode) {
        this.serverAuthCode = serverAuthCode;
    }
}
