package com.mowtiie.messecure.ui.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mowtiie.messecure.R;
import com.mowtiie.messecure.util.BootstrapHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private TextView nameText, emailText, memberSinceText, avatarLabel;
    private Chip statusChip;

    private FirebaseFirestore db;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db         = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getUid();

        avatarLabel = findViewById(R.id.avatarLabel);
        nameText = findViewById(R.id.nameText);
        emailText = findViewById(R.id.emailText);
        memberSinceText = findViewById(R.id.memberSinceText);
        statusChip = findViewById(R.id.statusChip);

        loadProfile();
    }

    private void loadProfile() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String name = doc.getString("displayName");
                    String email = doc.getString("email");
                    Boolean admin = doc.getBoolean("admin");
                    Boolean verified = doc.getBoolean("verified");
                    Date createdAt = doc.getDate("createdAt");

                    if (name != null) {
                        nameText.setText(name);
                        String[] parts = name.trim().split(" ");
                        String initials = parts.length >= 2
                                ? ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase()
                                : String.valueOf(name.charAt(0)).toUpperCase();
                        avatarLabel.setText(initials);
                    }
                    emailText.setText(email != null ? email : "");

                    if (Boolean.TRUE.equals(admin)) {
                        statusChip.setText("Admin");
                    } else if (Boolean.TRUE.equals(verified)) {
                        statusChip.setText("Verified");
                    } else {
                        statusChip.setText("Pending");
                    }

                    if (createdAt != null) {
                        memberSinceText.setText("Member since " +
                                new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                        .format(createdAt));
                    }
                });
    }
}