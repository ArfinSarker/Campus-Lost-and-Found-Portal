package com.sas.lostandfound;

import com.google.gson.annotations.SerializedName;

public class Conversation {
    @SerializedName("conversation_id")
    private String conversationId;

    @SerializedName("report_id")
    private String reportId;

    @SerializedName("item_name")
    private String itemName;

    @SerializedName("item_image_url")
    private String itemImageUrl;

    @SerializedName("item_type")
    private String itemType;

    @SerializedName("other_user_id")
    private String otherUserId;

    @SerializedName("other_user_name")
    private String otherUserName;

    @SerializedName("other_user_image_url")
    private String otherUserImageUrl;

    @SerializedName("last_message")
    private String lastMessage;

    @SerializedName("last_message_time")
    private String lastMessageTime;

    @SerializedName("unread_count")
    private int unreadCount;

    @SerializedName("other_user_last_active")
    private String otherUserLastActive;

    @SerializedName("last_message_sender_id")
    private String lastMessageSenderId;

    @SerializedName("last_message_is_read")
    private Boolean lastMessageIsRead;

    @SerializedName("last_message_is_delivered")
    private Boolean lastMessageIsDelivered;

    @SerializedName("request_status")
    private String requestStatus;

    @SerializedName("request_sender_id")
    private String requestSenderId;

    public Conversation() {}

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getItemImageUrl() { return itemImageUrl; }
    public void setItemImageUrl(String itemImageUrl) { this.itemImageUrl = itemImageUrl; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getOtherUserId() { return otherUserId; }
    public void setOtherUserId(String otherUserId) { this.otherUserId = otherUserId; }

    public String getOtherUserName() { return otherUserName; }
    public void setOtherUserName(String otherUserName) { this.otherUserName = otherUserName; }

    public String getOtherUserImageUrl() { return otherUserImageUrl; }
    public void setOtherUserImageUrl(String otherUserImageUrl) { this.otherUserImageUrl = otherUserImageUrl; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(String lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public String getOtherUserLastActive() { return otherUserLastActive; }
    public void setOtherUserLastActive(String otherUserLastActive) { this.otherUserLastActive = otherUserLastActive; }

    public String getLastMessageSenderId() { return lastMessageSenderId; }
    public void setLastMessageSenderId(String lastMessageSenderId) { this.lastMessageSenderId = lastMessageSenderId; }

    public Boolean getLastMessageIsRead() { return lastMessageIsRead != null ? lastMessageIsRead : false; }
    public void setLastMessageIsRead(Boolean lastMessageIsRead) { this.lastMessageIsRead = lastMessageIsRead; }

    public Boolean getLastMessageIsDelivered() { return lastMessageIsDelivered != null ? lastMessageIsDelivered : false; }
    public void setLastMessageIsDelivered(Boolean lastMessageIsDelivered) { this.lastMessageIsDelivered = lastMessageIsDelivered; }

    public String getRequestStatus() { return requestStatus != null ? requestStatus : "accepted"; }
    public void setRequestStatus(String requestStatus) { this.requestStatus = requestStatus; }

    public String getRequestSenderId() { return requestSenderId; }
    public void setRequestSenderId(String requestSenderId) { this.requestSenderId = requestSenderId; }

    public boolean isPendingRequest(String currentUserId) {
        return "pending".equals(requestStatus) && !currentUserId.equals(requestSenderId);
    }

    public boolean isRejectedRequest(String currentUserId) {
        return "rejected".equals(requestStatus) && !currentUserId.equals(requestSenderId);
    }
    
    public boolean isIncomingRequest(String currentUserId) {
        return ("pending".equals(requestStatus) || "rejected".equals(requestStatus)) && !currentUserId.equals(requestSenderId);
    }

    public boolean isOutgoingRequest(String currentUserId) {
        return ("pending".equals(requestStatus) || "rejected".equals(requestStatus)) && currentUserId.equals(requestSenderId);
    }
}
