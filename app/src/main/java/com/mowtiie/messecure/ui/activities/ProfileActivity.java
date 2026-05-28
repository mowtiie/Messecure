package com.mowtiie.messecure.ui.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mowtiie.messecure.R;

public class ProfileActivity extends AppCompatActivity {

    private TextView nameText, emailText, avatarLabel;
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

        db         = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getUid();

        nameText    = findViewById(R.id.nameText);
        emailText   = findViewById(R.id.emailText);
        avatarLabel = findViewById(R.id.avatarLabel);

        loadProfile();

        findViewById(R.id.editNameButton).setOnClickListener(v -> showEditNameDialog());
    }

    private void loadProfile() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    String name  = doc.getString("displayName");
                    String email = doc.getString("email");

                    nameText.setText(name != null ? name : "");
                    emailText.setText(email != null ? email : "");

                    if (name != null && !name.isEmpty()) {
                        String[] parts = name.trim().split(" ");
                        String initials = parts.length >= 2
                                ? String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)
                                : String.valueOf(parts[0].charAt(0));
                        avatarLabel.setText(initials.toUpperCase());
                    }
                });
    }

    private void showEditNameDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_edit_name, null);
        TextInputEditText nameInput = dialogView.findViewById(R.id.nameInput);
        nameInput.setText(nameText.getText());

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Display Name")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = nameInput.getText() != null
                            ? nameInput.getText().toString().trim() : "";
                    if (!newName.isEmpty()) {
                        db.collection("users").document(currentUid)
                                .update("displayName", newName)
                                .addOnSuccessListener(unused -> loadProfile());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}