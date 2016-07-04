package com.beeva.travelassistan;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by marian.claudiu on 4/6/16.
 */

public class Story {
    private String author;
    private String text;
    private double lat, lon;

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("author", getAuthor());
        result.put("text", getText());
        result.put("lat", getLat());
        result.put("lon", getLon());
        return result;
    }

    @Override
    public String toString() {
        return "Story{" +
                "author='" + author + '\'' +
                ", text='" + text + '\'' +
                ", lat=" + lat +
                ", lon=" + lon +
                '}';
    }
}
