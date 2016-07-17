package com.laxen.capmap.utils;

/**
 * Created by laxen on 6/28/16.
 */
public class VideoItem {
    private String latitude, longitude, url, location;

    public VideoItem() {

    }

    public VideoItem(String latitude, String longitude, String url, String location) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.url = url;
        this.location = location;
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

    public String getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "VideoItem{" +
                "url=" + url +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", location=" + location +
                '}';
    }
}
