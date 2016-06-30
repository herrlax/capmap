package com.laxen.capmap.network;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;

/**
 * Created by laxen on 6/30/16.
 */
public class DownloadManager {
    private final Context context;
    private String getUrl = "";
    private Response.Listener responseListener;
    private Response.ErrorListener errorListener;

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
        JsonArrayRequest jsObjRequest = new JsonArrayRequest
                (Request.Method.GET, getUrl, null, responseListener, errorListener);

        // Access the RequestQueue through your singleton class.
        RequestHandler.getInstance(context).addToRequestQueue(jsObjRequest);
    }

    public void setGetUrl(String getUrl) {
        this.getUrl = getUrl;
    }

}
