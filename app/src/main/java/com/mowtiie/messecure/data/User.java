package com.mowtiie.messecure.data;

public class User {
    private String userId;
    private String name;
    private String email;
    private boolean isVerified;
    private String avatarColorHex;

    public User(String userId, String name, String email, boolean isVerified, String avatarColorHex) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.isVerified = isVerified;
        this.avatarColorHex = avatarColorHex;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public String getAvatarColorHex() { return avatarColorHex; }
    public void setAvatarColorHex(String avatarColorHex) { this.avatarColorHex = avatarColorHex; }

    public String getInitials() {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length > 1) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return parts[0].substring(0, 1).toUpperCase();
    }
}
