package com.laxen.capmap.utils;

import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by laxen on 6/29/16.
 */
public class JsonHelper {

    // method for converting a jsonArray into a set of videoItems
    public static Set<VideoItem> jsonArrayToSet(JSONArray jsonArray) {

        Set<VideoItem> set = new HashSet<>();
        Gson gson = new Gson();

        for (int i=0; i<jsonArray.length(); i++) {

            try{
                VideoItem videoItem = gson.fromJson(jsonArray.get(i).toString(), VideoItem.class);
                set.add(videoItem);
            } catch (JSONException e) {
                Log.e("app", e.getMessage());
            }

        }

        return set;
    }
}
