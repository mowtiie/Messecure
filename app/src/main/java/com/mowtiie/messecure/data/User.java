package com.mowtiie.messecure.data;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class User {
    private String uid;
    private String displayName;
    private String email;
    private boolean verified;
    private boolean admin;
    private String fcmToken;
    @ServerTimestamp
    private Date createdAt;

    public User() {}

    public User(String uid, String displayName, String email) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
        this.verified = false; // new accounts start UNVERIFIED
        this.admin = false;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getAvatarLabel() {
        if (displayName == null || displayName.isEmpty()) return "?";
        String[] parts = displayName.trim().split(" ");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return String.valueOf(displayName.charAt(0)).toUpperCase();
    }

    // Status label for the admin dashboard
    public String getStatusLabel() {
        if (admin)    return "ADMIN";
        if (verified) return "Verified";
        return "Pending";
    }
}
