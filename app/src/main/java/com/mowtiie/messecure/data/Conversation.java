package com.mowtiie.messecure.data;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Conversation {

    private String id;
    private List<String> members;
    private String lastMessage;
    @ServerTimestamp
    private Date lastMessageTime;
    private int destructTimer;
    private String encryptionKey;

    private Map<String, Boolean> typing = new HashMap<>();

    private Map<String, Long> unreadCounts = new HashMap<>();

    private String otherUserName;
    private String otherUserEmail;
    private String otherUserId;

    public Conversation() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public Date getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(Date lastMessageTime) { this.lastMessageTime = lastMessageTime; }
    public int getDestructTimer() { return destructTimer; }
    public void setDestructTimer(int destructTimer) { this.destructTimer = destructTimer; }
    public String getEncryptionKey() { return encryptionKey; }
    public void setEncryptionKey(String encryptionKey) { this.encryptionKey = encryptionKey; }
    public Map<String, Boolean> getTyping() { return typing; }
    public void setTyping(Map<String, Boolean> typing) { this.typing = typing; }
    public Map<String, Long> getUnreadCounts() { return unreadCounts; }
    public void setUnreadCounts(Map<String, Long> unreadCounts) { this.unreadCounts = unreadCounts; }
    public String getOtherUserName() { return otherUserName; }
    public void setOtherUserName(String otherUserName) { this.otherUserName = otherUserName; }
    public String getOtherUserEmail() { return otherUserEmail; }
    public void setOtherUserEmail(String otherUserEmail) { this.otherUserEmail = otherUserEmail; }
    public String getOtherUserId() { return otherUserId; }
    public void setOtherUserId(String otherUserId) { this.otherUserId = otherUserId; }

    public String resolveOtherUserId(String currentUid) {
        if (members == null) return null;
        for (String uid : members) {
            if (!uid.equals(currentUid)) return uid;
        }
        return null;
    }

    public int getUnreadCountFor(String uid) {
        if (unreadCounts == null) return 0;
        Long v = unreadCounts.get(uid);
        return v != null ? v.intValue() : 0;
    }

    public boolean isOtherTyping(String currentUid) {
        if (typing == null) return false;
        for (Map.Entry<String, Boolean> e : typing.entrySet()) {
            if (!e.getKey().equals(currentUid) && Boolean.TRUE.equals(e.getValue())) {
                return true;
            }
        }
        return false;
    }

    public String getAvatarLabel() {
        if (otherUserName == null || otherUserName.isEmpty()) return "?";
        String[] parts = otherUserName.trim().split(" ");
        if (parts.length >= 2) return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        return String.valueOf(otherUserName.charAt(0)).toUpperCase();
    }
}
