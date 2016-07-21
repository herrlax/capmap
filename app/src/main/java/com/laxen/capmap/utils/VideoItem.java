package com.laxen.capmap.utils;

/**
 * Created by laxen on 6/28/16.
 */
public class VideoItem {
    private String latitude, longitude, url, location, thumbnail, timestamp, expires;

    public VideoItem() {

    }

    public VideoItem(String latitude, String longitude,
                     String url, String location, String thumbnail,
                     String timestamp, String expires) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.url = url;
        this.location = location;
        this.thumbnail = thumbnail;
        this.timestamp = timestamp;
        this.expires = expires;
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

    public String getThumbnail() {
        return thumbnail;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getExpires() { return expires; }

    @Override
    public String toString() {
        return "VideoItem{" +
                "url=" + url +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", location=" + location +
                ", thumbnail=" + thumbnail +
                ", timestamp=" + timestamp +
                ", expires=" + expires +
                '}';
    }
}
