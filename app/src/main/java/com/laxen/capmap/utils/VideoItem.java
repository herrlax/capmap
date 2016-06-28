package com.laxen.capmap.utils;

/**
 * Created by laxen on 6/28/16.
 */
public class VideoItem {
    private Integer lat, lon;
    private String videoUrl;

    public VideoItem() {

    }

    public VideoItem(Integer lat, Integer lon, String videoUrl) {
        this.lat = lat;
        this.lon = lon;
        this.videoUrl = videoUrl;
    }


    public Integer getLat() {
        return lat;
    }

    public Integer getLon() {
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
