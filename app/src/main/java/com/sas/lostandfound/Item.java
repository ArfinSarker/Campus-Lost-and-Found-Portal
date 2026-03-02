package com.sas.lostandfound;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Item {
    private String id;
    private String name;
    private String category;
    private String description;
    private String location;
    private String date;
    private String imageUrl;
    private String status; // "lost" or "found"
    private String userId;
    private String userName;
    private String userEmail;

    public Item() {
        // Required for Firebase
    }

    public Item(String id, String name, String category, String description, String location, String date, String imageUrl, String status, String userId) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.location = location;
        this.date = date;
        this.imageUrl = imageUrl;
        this.status = status;
        this.userId = userId;
    }

    // Constructor for temporary/sample data if needed (mapping from old ItemInformationActivity)
    public Item(String id, String name, String location, String date, String status, String imageUrl) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.date = date;
        this.status = status;
        this.imageUrl = imageUrl;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
}
