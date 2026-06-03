package com.mowtiie.messecure.util;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TypingHelper {

    public static final long STALE_MS = 3000;

    public static void setTyping(String convId) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || convId == null) return;
        Map<String, Object> update = new HashMap<>();
        update.put("typing." + uid, System.currentTimeMillis());
        FirebaseFirestore.getInstance()
                .collection("conversations").document(convId)
                .update(update);
    }

    public static void clearTyping(String convId) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || convId == null) return;
        Map<String, Object> update = new HashMap<>();
        update.put("typing." + uid, FieldValue.delete());
        FirebaseFirestore.getInstance()
                .collection("conversations").document(convId)
                .update(update);
    }

    public static boolean isStillTyping(Long entry) {
        if (entry == null) return false;
        return System.currentTimeMillis() - entry < STALE_MS;
    }
}