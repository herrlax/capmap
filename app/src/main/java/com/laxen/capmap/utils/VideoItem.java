package com.laxen.capmap.utils;

/**
 * Created by laxen on 6/28/16.
 */
public class VideoItem {
    private Double lat, lon;
    private String videoUrl;

    public VideoItem() {

    }

    public VideoItem(Double lat, Double lon, String videoUrl) {
        this.lat = lat;
        this.lon = lon;
        this.videoUrl = videoUrl;
    }


    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    @Override
    public String toString() {
        return "VideoItem{" +
                "lat=" + lat +
                ", lon='" + lon +
                ", videoUrl=" + videoUrl +
                '}';
    }
}
