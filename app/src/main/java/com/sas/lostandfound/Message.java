package com.sas.lostandfound;

import com.google.gson.annotations.SerializedName;

public class Message {
    @SerializedName("id")
    private String id;

    @SerializedName("conversation_id")
    private String conversationId;

    @SerializedName("sender_id")
    private String senderId;

    @SerializedName("message_text")
    private String messageText;

    @SerializedName("is_read")
    private boolean isRead;

    @SerializedName("is_delivered")
    private boolean isDelivered;

    @SerializedName("receiver_marked_unread")
    private boolean receiverMarkedUnread;

    @SerializedName("created_at")
    private String createdAt;

    public Message() {}

    public Message(String conversationId, String senderId, String messageText) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.messageText = messageText;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public boolean isDelivered() { return isDelivered; }
    public void setDelivered(boolean delivered) { isDelivered = delivered; }

    public boolean isReceiverMarkedUnread() { return receiverMarkedUnread; }
    public void setReceiverMarkedUnread(boolean receiverMarkedUnread) { this.receiverMarkedUnread = receiverMarkedUnread; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
