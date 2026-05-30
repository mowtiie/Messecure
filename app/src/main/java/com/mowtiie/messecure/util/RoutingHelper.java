package com.mowtiie.messecure.util;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mowtiie.messecure.ui.activities.LoginActivity;
import com.mowtiie.messecure.ui.activities.MainActivity;
import com.mowtiie.messecure.ui.activities.PendingApprovalActivity;

public class RoutingHelper {

    public interface RouteCallback {
        void onRouteResolved(Intent intent);
    }

    public static void resolveDestination(@NonNull Context context,
                                          @NonNull RouteCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onRouteResolved(new Intent(context, LoginActivity.class));
            return;
        }

        user.getIdToken(true).addOnSuccessListener(result -> {
            boolean isAdmin = result.getClaims().get("admin") != null
                    && Boolean.TRUE.equals(result.getClaims().get("admin"));

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        boolean verified = doc.exists()
                                && Boolean.TRUE.equals(doc.getBoolean("verified"));

                        Intent intent;
                        if (verified || isAdmin) {
                            intent = new Intent(context, MainActivity.class);
                            intent.putExtra("isAdmin", isAdmin);
                        } else {
                            intent = new Intent(context, PendingApprovalActivity.class);
                        }
                        callback.onRouteResolved(intent);
                    })
                    .addOnFailureListener(e -> {
                        callback.onRouteResolved(new Intent(context, PendingApprovalActivity.class));
                    });
        }).addOnFailureListener(e ->
                callback.onRouteResolved(new Intent(context, LoginActivity.class)));
    }

}
