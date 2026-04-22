package com.sas.lostandfound;

import com.google.firebase.database.IgnoreExtraProperties;
import java.util.ArrayList;
import java.util.List;

@IgnoreExtraProperties
public class AdminReport {
    private String reportId;
    private String displayId; // e.g., R1, R2
    private String title;
    private String category;
    private String description;
    private String relatedId;
    private String reporterName;
    private String universityId;
    private String reporterAuthId;
    private String phone;
    private String imageUrl; // For backward compatibility (first image)
    private List<String> imageUrls;
    private String priority; // Low, Medium, High
    private String status; // Pending, Reviewed, Resolved
    private String adminNote;
    private long timestamp;
    private long updatedAt;
    private boolean deletedByUser;

    public AdminReport() {
        this.imageUrls = new ArrayList<>();
    }

    public AdminReport(String reportId, String displayId, String title, String category, String description, String relatedId, String reporterName, String universityId, String reporterAuthId, String phone, String imageUrl, String priority, String status, long timestamp) {
        this.reportId = reportId;
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
        this.imageUrls = new ArrayList<>();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            this.imageUrls.add(imageUrl);
        }
        this.priority = priority;
        this.status = status;
        this.timestamp = timestamp;
        this.updatedAt = timestamp;
        this.deletedByUser = false;
    }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getDisplayId() { return displayId; }
    public void setDisplayId(String displayId) { this.displayId = displayId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRelatedId() { return relatedId; }
    public void setRelatedId(String relatedId) { this.relatedId = relatedId; }
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    public String getUniversityId() { return universityId; }
    public void setUniversityId(String universityId) { this.universityId = universityId; }
    public String getReporterAuthId() { return reporterAuthId; }
    public void setReporterAuthId(String reporterAuthId) { this.reporterAuthId = reporterAuthId; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public boolean isDeletedByUser() { return deletedByUser; }
    public void setDeletedByUser(boolean deletedByUser) { this.deletedByUser = deletedByUser; }

    // Compatibility getters
    public long getCreatedAt() { return timestamp; }
    public String getRelatedReportId() { return relatedId; }
    public String getContactPhone() { return phone; }
}
