package com.sas.lostandfound;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class AdminReport {
    private String reportId;
    private String displayId; // e.g., R1, R2
    private String title;
    private String category;
    private String description;
    private String relatedReportId;
    private String reporterName;
    private String universityId;
    private String userId; // Firebase Auth UID for security rules
    private String contactPhone;
    private String imageUrl;
    private String priority; // Low, Medium, High
    private String status; // Pending, Reviewed, Resolved
    private String adminNote;
    private long createdAt;
    private long updatedAt;

    public AdminReport() {
    }

    public AdminReport(String reportId, String displayId, String title, String category, String description, String relatedReportId, String reporterName, String universityId, String userId, String contactPhone, String imageUrl, String priority, String status, long createdAt) {
        this.reportId = reportId;
        this.displayId = displayId;
        this.title = title;
        this.category = category;
        this.description = description;
        this.relatedReportId = relatedReportId;
        this.reporterName = reporterName;
        this.universityId = universityId;
        this.userId = userId;
        this.contactPhone = contactPhone;
        this.imageUrl = imageUrl;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
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
    public String getRelatedReportId() { return relatedReportId; }
    public void setRelatedReportId(String relatedReportId) { this.relatedReportId = relatedReportId; }
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    public String getUniversityId() { return universityId; }
    public void setUniversityId(String universityId) { this.universityId = universityId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
