package com.sas.lostandfound;

import com.google.gson.annotations.SerializedName;

public class ChatRequest {
    @SerializedName("request_id")
    private String requestId;

    @SerializedName("sender_id")
    private String senderId;

    @SerializedName("sender_name")
    private String senderName;

    @SerializedName("sender_image_url")
    private String senderImageUrl;

    @SerializedName("receiver_id")
    private String receiverId;

    @SerializedName("receiver_name")
    private String receiverName;

    @SerializedName("report_id")
    private String reportId;

    @SerializedName("item_name")
    private String itemName;

    @SerializedName("item_image_url")
    private String itemImageUrl;

    @SerializedName("item_type")
    private String itemType;

    @SerializedName("initial_message")
    private String initialMessage;

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    public ChatRequest() {}

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderImageUrl() { return senderImageUrl; }
    public void setSenderImageUrl(String senderImageUrl) { this.senderImageUrl = senderImageUrl; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getItemImageUrl() { return itemImageUrl; }
    public void setItemImageUrl(String itemImageUrl) { this.itemImageUrl = itemImageUrl; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getInitialMessage() { return initialMessage; }
    public void setInitialMessage(String initialMessage) { this.initialMessage = initialMessage; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
