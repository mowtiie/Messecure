package com.mowtiie.messecure.data;

public class Message {

    private String messageId;
    private String conversationId;
    private String senderId;
    private String encryptedPayload;
    private long timestamp;
    private boolean isRead;

    private boolean isSelfDestructing;
    private long selfDestructDurationMs;
    private long timerStartedTimestamp;

    public Message(String messageId, String conversationId, String senderId, String encryptedPayload, long timestamp, boolean isRead) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.encryptedPayload = encryptedPayload;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.isSelfDestructing = false;
        this.selfDestructDurationMs = 0;
        this.timerStartedTimestamp = 0;
    }

    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getEncryptedPayload() { return encryptedPayload; }
    public void setEncryptedPayload(String encryptedPayload) { this.encryptedPayload = encryptedPayload; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public boolean isSelfDestructing() { return isSelfDestructing; }
    public void setSelfDestructing(boolean selfDestructing) { isSelfDestructing = selfDestructing; }

    public long getSelfDestructDurationMs() { return selfDestructDurationMs; }
    public void setSelfDestructDurationMs(long selfDestructDurationMs) { this.selfDestructDurationMs = selfDestructDurationMs; }

    public long getTimerStartedTimestamp() { return timerStartedTimestamp; }
    public void setTimerStartedTimestamp(long timerStartedTimestamp) { this.timerStartedTimestamp = timerStartedTimestamp; }

    public long getRemainingTimeMs() {
        if (!isSelfDestructing || timerStartedTimestamp == 0) {
            return selfDestructDurationMs;
        }
        long timeElapsed = System.currentTimeMillis() - timerStartedTimestamp;
        return selfDestructDurationMs - timeElapsed;
    }
}