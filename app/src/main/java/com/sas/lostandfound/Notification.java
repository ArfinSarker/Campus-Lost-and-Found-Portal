package com.sas.lostandfound;

import com.google.gson.annotations.SerializedName;

/**
 * Notification model representing a system or user-to-user notification.
 * Fields match the standardized notifications table in Supabase.
 */
public class Notification {
    @SerializedName("id")
    private String id;
    
    @SerializedName("recipient_id")
    private String recipientId;
    
    @SerializedName("sender_id")
    private String senderId;
    
    @SerializedName("report_id")
    private String reportId;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("type")
    private String type;
    
    @SerializedName("is_read")
    private boolean isRead;
    
    @SerializedName("additional_details")
    private String additionalDetails;
    
    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("sender_name")
    private String senderName;

    @SerializedName("sender_phone")
    private String senderPhone;

    @SerializedName("sender_email")
    private String senderEmail;

    @SerializedName("sender_image_url")
    private String senderImageUrl;

    @SerializedName("item_name")
    private String itemName;

    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("user_id")
    private String userId; // Recipient's Auth ID for RLS

    // --- COMPATIBILITY FIELDS (Stored in DB for adapter/legacy support) ---
    
    @SerializedName("claimer_id")
    private String claimerId;

    @SerializedName("claimer_name")
    private String claimerName;

    @SerializedName("item_id")
    private String itemIdField;

    @SerializedName("claim_type")
    private String claimType;

    public Notification() {
    }

    /**
     * Standard constructor for most notifications.
     */
    public Notification(String id, String recipientId, String senderId, String senderName, String senderPhone, String senderEmail, String senderImageUrl, String reportId, String itemName, String message, long timestamp, String type, String additionalDetails) {
        this.id = id;
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderPhone = senderPhone;
        this.senderEmail = senderEmail;
        this.senderImageUrl = senderImageUrl;
        this.reportId = reportId;
        this.itemName = itemName;
        this.message = message;
        this.timestamp = timestamp;
        this.type = type;
        this.additionalDetails = additionalDetails;
        this.isRead = false;
        
        // Populate compatibility fields for the adapter/legacy flows
        this.claimerId = senderId;
        this.claimerName = senderName;
        this.itemIdField = reportId;
        this.claimType = type;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    
    /**
     * Helper for adapter compatibility. Returns itemIdField if available, else reportId.
     */
    public String getItemId() { 
        return (itemIdField != null && !itemIdField.isEmpty()) ? itemIdField : reportId; 
    }
    public void setItemId(String itemId) { this.reportId = itemId; this.itemIdField = itemId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getAdditionalDetails() { return additionalDetails; }
    public void setAdditionalDetails(String details) { this.additionalDetails = details; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String name) { this.senderName = name; this.claimerName = name; }
    
    public String getSenderPhone() { return senderPhone; }
    public void setSenderPhone(String phone) { this.senderPhone = phone; }
    
    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String email) { this.senderEmail = email; }

    public String getSenderImageUrl() { return senderImageUrl; }
    public void setSenderImageUrl(String url) { this.senderImageUrl = url; }
    
    public String getItemName() { return itemName; }
    public void setItemName(String name) { this.itemName = name; }

    public String getClaimerId() { return claimerId; }
    public void setClaimerId(String id) { this.claimerId = id; }

    public String getClaimerName() { return claimerName; }
    public void setClaimerName(String name) { this.claimerName = name; }

    public String getItemIdField() { return itemIdField; }
    public void setItemIdField(String id) { this.itemIdField = id; }

    public String getClaimType() { return claimType; }
    public void setClaimType(String type) { this.claimType = type; }

    public String getUserId() { return userId; }
    public void setUserId(String id) { this.userId = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long ts) { this.timestamp = ts; }
}
