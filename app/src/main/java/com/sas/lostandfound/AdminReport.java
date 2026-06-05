package com.sas.lostandfound;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class AdminReport {
    @SerializedName("id")
    private String id;
    
    @SerializedName("display_id")
    private String displayId;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("category")
    private String category;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("related_item_id")
    private String relatedId;
    
    @SerializedName("reporter_name")
    private String reporterName;
    
    @SerializedName("reporter_id")
    private String universityId;
    
    @SerializedName("user_id")
    private String reporterAuthId;
    
    @SerializedName("phone")
    private String phone;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("status")
    private String status;
    
    @SerializedName("admin_note")
    private String adminNote;
    
    @SerializedName("image_urls")
    private List<String> imageUrls;

    @SerializedName("image_url")
    private String imageUrl;
    
    @SerializedName("timestamp")
    private long timestamp;
    
    @SerializedName("updated_at_timestamp")
    private long updatedAt;

    @SerializedName("deleted_by_user")
    private boolean deletedByUser;

    @SerializedName("reviewed_by")
    private String reviewedBy;

    @SerializedName("review_timestamp")
    private Long reviewTimestamp;

    public AdminReport() {
        this.imageUrls = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public AdminReport(String id, String title, String category, String description, String relatedId, String reporterAuthId) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.description = description;
        this.relatedId = relatedId;
        this.reporterAuthId = reporterAuthId;
        this.status = "Pending";
        this.imageUrls = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    public AdminReport(String id, String displayId, String title, String category, String description, String relatedId, String reporterName, String universityId, String reporterAuthId, String phone, String imageUrl, String status, long timestamp) {
        this.id = id;
        this.displayId = displayId;
        this.title = title;
        this.category = category;
        this.description = description;
        this.relatedId = relatedId;
        this.reporterName = reporterName;
        this.universityId = universityId;
        this.reporterAuthId = reporterAuthId;
        this.phone = phone;
        this.imageUrl = imageUrl;
        this.status = status;
        this.timestamp = timestamp;
        this.imageUrls = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getReportId() { return id; } // Compatibility
    public void setReportId(String id) { this.id = id; }

    public String getDisplayId() { return displayId; }
    public void setDisplayId(String displayId) { this.displayId = displayId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRelatedId() { return relatedId; }
    public void setRelatedId(String id) { this.relatedId = id; }

    public String getReporterAuthId() { return reporterAuthId; }
    public void setReporterAuthId(String id) { this.reporterAuthId = id; }



    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String note) { this.adminNote = note; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> urls) { this.imageUrls = urls; }

    public String getImageUrl() { 
        if (imageUrl != null) return imageUrl;
        return (imageUrls != null && !imageUrls.isEmpty()) ? imageUrls.get(0) : null; 
    }
    public void setImageUrl(String url) {
        this.imageUrl = url;
        if (imageUrls == null) imageUrls = new ArrayList<>();
        if (url != null && !imageUrls.contains(url)) imageUrls.add(0, url);
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long ts) { this.timestamp = ts; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long ts) { this.updatedAt = ts; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String name) { this.reporterName = name; }
    
    public String getUniversityId() { return universityId; }
    public void setUniversityId(String id) { this.universityId = id; }
    
    public String getPhone() { return phone; }
    public void setPhone(String p) { this.phone = p; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isDeletedByUser() { return deletedByUser; }
    public void setDeletedByUser(boolean deletedByUser) { this.deletedByUser = deletedByUser; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public Long getReviewTimestamp() { return reviewTimestamp; }
    public void setReviewTimestamp(Long reviewTimestamp) { this.reviewTimestamp = reviewTimestamp; }

    private transient String reporterProfileImageUrl;
    public String getReporterProfileImageUrl() { return reporterProfileImageUrl; }
    public void setReporterProfileImageUrl(String url) { this.reporterProfileImageUrl = url; }
}
