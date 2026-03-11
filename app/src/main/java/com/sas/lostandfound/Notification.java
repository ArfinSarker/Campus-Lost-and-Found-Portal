package com.sas.lostandfound;

public class Notification {
    private String id;
    private String recipientId;
    private String senderId;
    private String senderName;
    private String senderPhone;
    private String senderEmail;
    private String itemId;
    private String itemName;
    private String message;
    private long timestamp;
    private boolean read;
    private String type; // "lost_claim" or "found_claim"
    private String additionalDetails;

    public Notification() {
    }

    public Notification(String id, String recipientId, String senderId, String senderName, String senderPhone, String senderEmail, String itemId, String itemName, String message, long timestamp, String type, String additionalDetails) {
        this.id = id;
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderPhone = senderPhone;
        this.senderEmail = senderEmail;
        this.itemId = itemId;
        this.itemName = itemName;
        this.message = message;
        this.timestamp = timestamp;
        this.read = false;
        this.type = type;
        this.additionalDetails = additionalDetails;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderPhone() { return senderPhone; }
    public void setSenderPhone(String senderPhone) { this.senderPhone = senderPhone; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAdditionalDetails() { return additionalDetails; }
    public void setAdditionalDetails(String additionalDetails) { this.additionalDetails = additionalDetails; }
}
