package com.mowtiie.messecure.util;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UnreadCountHelper {

    public static Task<Void> markAsRead(String convId) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || convId == null) return null;

        Map<String, Object> update = new HashMap<>();
        update.put("lastRead." + convId, System.currentTimeMillis());
        return FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .update(update);
    }

    public static Task<QuerySnapshot> queryUnread(String convId, long lastReadMs) {
        String uid = FirebaseAuth.getInstance().getUid();
        Task<QuerySnapshot> querySnapshotTask = FirebaseFirestore.getInstance()
                .collection("conversations").document(convId)
                .collection("messages")
                .whereGreaterThan("sentAt", new Date(lastReadMs))
                .whereNotEqualTo("senderId", uid)
                .get();
        return querySnapshotTask;
    }

    public static long getLastRead(Map<String, Object> userDocData, String convId) {
        if (userDocData == null) return 0L;
        Object lr = userDocData.get("lastRead");
        if (!(lr instanceof Map)) return 0L;
        Object v = ((Map<?, ?>) lr).get(convId);
        if (v instanceof Number) return ((Number) v).longValue();
        return 0L;
    }
}