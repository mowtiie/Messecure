package com.mowtiie.messecure.data;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;

public class Conversation {
    private String id;
    private List<String> members;
    private String lastMessage;
    @ServerTimestamp
    private Date lastMessageTime;
    private int destructTimer;

    private String otherUserName;
    private String otherUserEmail;

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

    public String getOtherUserName() { return otherUserName; }
    public void setOtherUserName(String otherUserName) { this.otherUserName = otherUserName; }

    public String getOtherUserEmail() { return otherUserEmail; }
    public void setOtherUserEmail(String otherUserEmail) { this.otherUserEmail = otherUserEmail; }

    public String getOtherUserId(String currentUid) {
        if (members == null) return null;
        for (String uid : members) {
            if (!uid.equals(currentUid)) return uid;
        }
        return null;
    }

    public String getAvatarLabel() {
        if (otherUserName == null || otherUserName.isEmpty()) return "?";
        String[] parts = otherUserName.trim().split(" ");
        if (parts.length >= 2) {
            return String.valueOf(parts[0].charAt(0)).toUpperCase()
                 + String.valueOf(parts[1].charAt(0)).toUpperCase();
        }
        return String.valueOf(otherUserName.charAt(0)).toUpperCase();
    }
}
