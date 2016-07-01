package com.laxen.capmap.utils;

/**
 * Created by laxen on 6/28/16.
 */
public class VideoItem {
    private String latitude, longitude, url;

    public VideoItem() {

    }

    public VideoItem(String latitude, String longitude, String url) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.url = url;
    }


    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "VideoItem{" +
                "url=" + url +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
