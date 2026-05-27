package com.mowtiie.messecure.data;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Message {
    private String id;
    private String senderId;
    private String text;
    private String decryptedText;
    @ServerTimestamp
    private Date sentAt;
    private Date readAt;
    private boolean selfDestruct;
    private int destructAfterMinutes;

    public Message() {}

    public Message(String senderId, String encryptedText, boolean selfDestruct, int destructAfterMinutes) {
        this.senderId = senderId;
        this.text = encryptedText;
        this.selfDestruct = selfDestruct;
        this.destructAfterMinutes = destructAfterMinutes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getDecryptedText() { return decryptedText; }
    public void setDecryptedText(String decryptedText) { this.decryptedText = decryptedText; }

    public Date getSentAt() { return sentAt; }
    public void setSentAt(Date sentAt) { this.sentAt = sentAt; }

    public Date getReadAt() { return readAt; }
    public void setReadAt(Date readAt) { this.readAt = readAt; }

    public boolean isSelfDestruct() { return selfDestruct; }
    public void setSelfDestruct(boolean selfDestruct) { this.selfDestruct = selfDestruct; }

    public int getDestructAfterMinutes() { return destructAfterMinutes; }
    public void setDestructAfterMinutes(int destructAfterMinutes) { this.destructAfterMinutes = destructAfterMinutes; }

    public boolean isSentByCurrentUser(String currentUid) {
        return senderId != null && senderId.equals(currentUid);
    }

    public boolean isRead() {
        return readAt != null;
    }
}
