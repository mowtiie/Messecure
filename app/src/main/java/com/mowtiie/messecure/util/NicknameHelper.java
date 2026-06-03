package com.mowtiie.messecure.util;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class NicknameHelper {

    public static String getNickname(Map<String, Object> currentUserDocData, String otherUid) {
        if (currentUserDocData == null || otherUid == null) return null;
        Object n = currentUserDocData.get("nicknames");
        if (!(n instanceof Map)) return null;
        Object v = ((Map<?, ?>) n).get(otherUid);
        return v != null ? v.toString() : null;
    }

    public static String resolveLabel(String nickname, String realName) {
        return (nickname != null && !nickname.isEmpty()) ? nickname : realName;
    }

    public static Task<Void> setNickname(String otherUid, String nickname) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || otherUid == null) return null;

        Map<String, Object> update = new HashMap<>();
        if (nickname == null || nickname.trim().isEmpty()) {
            update.put("nicknames." + otherUid, FieldValue.delete());
        } else {
            update.put("nicknames." + otherUid, nickname.trim());
        }
        return FirebaseFirestore.getInstance()
                .collection("users").document(uid).update(update);
    }

}
