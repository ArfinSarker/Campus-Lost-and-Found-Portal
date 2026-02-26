package com.sas.lostandfound;

public class ItemActivity {
    private String name;
    private String location;
    private String timeAgo;
    private String icon; // can be emoji or resource name

    public ItemActivity(String name, String location, String timeAgo, String icon) {
        this.name = name;
        this.location = location;
        this.timeAgo = timeAgo;
        this.icon = icon;
    }

    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getTimeAgo() { return timeAgo; }
    public String getIcon() { return icon; }
}