package com.mowtiie.messecure;

import android.app.Application;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;
import com.google.firebase.functions.FirebaseFunctions;
import com.mowtiie.messecure.util.KeystoreHelper;

public class MessecureApp extends Application {

    private static final boolean USE_EMULATOR = true;
    private static final String EMULATOR_HOST = "192.168.1.69";

    @Override
    public void onCreate() {
        super.onCreate();

        if (USE_EMULATOR) {
            FirebaseAuth.getInstance().useEmulator(EMULATOR_HOST, 9099);
            FirebaseFirestore.getInstance().useEmulator(EMULATOR_HOST, 8080);
            FirebaseFunctions.getInstance().useEmulator(EMULATOR_HOST, 5001);
        } else {
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                            PersistentCacheSettings.newBuilder()
                                    .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                                    .build()
                    )
                    .build();
            FirebaseFirestore.getInstance().setFirestoreSettings(settings);
        }

        try {
            KeystoreHelper.generateKey();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
