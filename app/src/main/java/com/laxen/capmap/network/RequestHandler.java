package com.laxen.capmap.network;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by laxen on 6/28/16.
 * Class for handling request queue of network traffic
 */
public class RequestHandler {
    private static RequestHandler instance;
    private RequestQueue queue;
    private static Context context;

    private RequestHandler(final Context context) {
        this.context = context;
        queue = getRequestQueue();

    }

    public static synchronized RequestHandler getInstance(Context context) {
        if (instance == null) {
            instance = new RequestHandler(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (queue == null)
            queue = Volley.newRequestQueue(context.getApplicationContext());

        return queue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

}
