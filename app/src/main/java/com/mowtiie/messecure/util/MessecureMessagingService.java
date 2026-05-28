package com.mowtiie.messecure.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.mowtiie.messecure.ui.activities.ChatActivity;
import com.mowtiie.messecure.R;

public class MessecureMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID   = "messecure_secure";
    private static final String CHANNEL_NAME = "Secure Messages";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        String convId = remoteMessage.getData().get("convId");
        if (convId == null) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean stealthMode = prefs.getBoolean("stealth_notif", true);

        String title;
        String body;

        if (stealthMode) {
            title = "Messecure";
            body  = "You have a new secure message";
        } else {
            String senderName = remoteMessage.getData().get("senderName");
            title = senderName != null ? senderName : "Messecure";
            body  = "Tap to read";
        }

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("convId", convId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_lock)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        if (stealthMode) {
            builder.setVisibility(NotificationCompat.VISIBILITY_SECRET);
        }

        NotificationManagerCompat.from(this).notify(convId.hashCode(), builder.build());
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update("fcmToken", token);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Stealth notifications for Messecure");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}
