package com.mowtiie.messecure;

import android.app.Application;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;
import com.google.firebase.functions.FirebaseFunctions;
import com.mowtiie.messecure.util.KeystoreHelper;

public class MessecureApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseFunctions.getInstance().useEmulator("192.168.1.69", 5001);
        FirebaseFunctions.getInstance().useEmulator("192.168.1.69", 8080);

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
