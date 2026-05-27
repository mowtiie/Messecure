package com.mowtiie.messecure;

import android.app.Application;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;
import com.mowtiie.messecure.util.KeystoreHelper;

public class MessecureApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PersistentCacheSettings cacheSettings = PersistentCacheSettings.newBuilder()
                .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(cacheSettings)
                .build();

        FirebaseFirestore.getInstance().setFirestoreSettings(settings);

        try {
            KeystoreHelper.generateKey();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
