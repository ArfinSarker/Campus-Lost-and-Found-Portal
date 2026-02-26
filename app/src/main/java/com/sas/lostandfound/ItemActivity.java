package com.sas.lostandfound;

public class ItemActivity {
    private int id;
    private String title;
    private String location;
    private String timeAgo;
    private String status; // "lost" or "found"
    private String imageUrl;

    public ItemActivity(int id, String title, String location, String timeAgo, String status, String imageUrl) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.timeAgo = timeAgo;
        this.status = status;
        this.imageUrl = imageUrl;
    }

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getLocation() { return location; }
    public String getTimeAgo() { return timeAgo; }
    public String getStatus() { return status; }
    public String getImageUrl() { return imageUrl; }
}