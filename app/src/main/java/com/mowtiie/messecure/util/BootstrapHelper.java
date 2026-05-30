package com.mowtiie.messecure.util;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;

public class BootstrapHelper {

    private static final String REGION = "asia-southeast1";

    public static void promoteSelf(Context context) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(context, "Sign in first.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFunctions.getInstance(REGION)
                .getHttpsCallable("bootstrapAdmin")
                .call()
                .addOnSuccessListener(result -> Toast.makeText(context,
                        "Admin granted. Sign out and back in to activate.",
                        Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(context,
                        "Bootstrap failed: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }

