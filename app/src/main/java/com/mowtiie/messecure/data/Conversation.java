package com.mowtiie.messecure.data;

public class Conversation {
    private String conversationId;
    private User participant;
    private String lastMessageText;
    private long lastMessageTimestamp;
    private int unreadCount;
    private boolean isEncrypted;

    public Conversation(String conversationId, User participant, String lastMessageText, long lastMessageTimestamp, int unreadCount, boolean isEncrypted) {
        this.conversationId = conversationId;
        this.participant = participant;
        this.lastMessageText = lastMessageText;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.unreadCount = unreadCount;
        this.isEncrypted = isEncrypted;
    }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public User getParticipant() { return participant; }
    public void setParticipant(User participant) { this.participant = participant; }

    public String getLastMessageText() { return lastMessageText; }
    public void setLastMessageText(String lastMessageText) { this.lastMessageText = lastMessageText; }

    public long getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(long lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public boolean isEncrypted() { return isEncrypted; }
    public void setEncrypted(boolean encrypted) { isEncrypted = encrypted; }
}