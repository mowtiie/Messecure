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
    private Date expireAt;
    private boolean selfDestruct;
    private int destructAfterMinutes;

    private String replyToId;
    private String replyToSnippet;
    private String replyToSender;

    private String decryptedReplySnippet;

    public Message() {}

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
    public Date getExpireAt() { return expireAt; }
    public void setExpireAt(Date expireAt) { this.expireAt = expireAt; }
    public boolean isSelfDestruct() { return selfDestruct; }
    public void setSelfDestruct(boolean selfDestruct) { this.selfDestruct = selfDestruct; }
    public int getDestructAfterMinutes() { return destructAfterMinutes; }
    public void setDestructAfterMinutes(int destructAfterMinutes) { this.destructAfterMinutes = destructAfterMinutes; }
    public String getReplyToId() { return replyToId; }
    public void setReplyToId(String replyToId) { this.replyToId = replyToId; }
    public String getReplyToSnippet() { return replyToSnippet; }
    public void setReplyToSnippet(String replyToSnippet) { this.replyToSnippet = replyToSnippet; }
    public String getReplyToSender() { return replyToSender; }
    public void setReplyToSender(String replyToSender) { this.replyToSender = replyToSender; }
    public String getDecryptedReplySnippet() { return decryptedReplySnippet; }
    public void setDecryptedReplySnippet(String s) { this.decryptedReplySnippet = s; }

    public boolean isSentByCurrentUser(String currentUid) {
        return senderId != null && senderId.equals(currentUid);
    }
    public boolean isRead() { return readAt != null; }
    public boolean isReply() { return replyToId != null; }
}
