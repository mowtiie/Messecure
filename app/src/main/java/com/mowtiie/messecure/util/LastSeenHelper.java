package com.mowtiie.messecure.util;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LastSeenHelper {

    public static void touch() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("lastSeen", FieldValue.serverTimestamp());
    }

    public static String format(Date lastSeen) {
        if (lastSeen == null) return "Last seen recently";

        long diffMs   = System.currentTimeMillis() - lastSeen.getTime();
        long diffSec  = diffMs / 1000;
        long diffMin  = diffSec / 60;
        long diffHour = diffMin / 60;

        if (diffSec < 60)  return "Online";
        if (diffMin < 60)  return "Last seen " + diffMin + " minute" + (diffMin == 1 ? "" : "s") + " ago";
        if (diffHour < 24) return "Last seen at " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(lastSeen);
        return "Last seen " + new SimpleDateFormat("MMM d", Locale.getDefault()).format(lastSeen);
    }

    public static boolean isOnline(Date lastSeen) {
        if (lastSeen == null) return false;
        return System.currentTimeMillis() - lastSeen.getTime() < 60_000;
    }
}