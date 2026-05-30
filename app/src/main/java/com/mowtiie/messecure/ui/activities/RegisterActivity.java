package com.mowtiie.messecure.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mowtiie.messecure.R;
import com.mowtiie.messecure.data.User;
import com.mowtiie.messecure.util.BackupCodeHelper;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String ALLOWED_DOMAIN = "@sti.edu.ph";

    private TextInputEditText nameField, emailField, passwordField, confirmPasswordField;
    private Button registerButton;
    private ProgressBar progressBar;
    private View rootView;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        rootView             = findViewById(R.id.rootView);
        nameField            = findViewById(R.id.nameField);
        emailField           = findViewById(R.id.emailField);
        passwordField        = findViewById(R.id.passwordField);
        confirmPasswordField = findViewById(R.id.confirmPasswordField);
        registerButton       = findViewById(R.id.registerButton);
        progressBar          = findViewById(R.id.progressBar);

        registerButton.setOnClickListener(v -> attemptRegister());

        TextView loginLink = findViewById(R.id.loginLink);
        loginLink.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String name     = text(nameField);
        String email    = text(emailField);
        String password = text(passwordField);
        String confirm  = text(confirmPasswordField);

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }
        if (!email.endsWith(ALLOWED_DOMAIN)) {
            showError("Only " + ALLOWED_DOMAIN + " email addresses are allowed.");
            return;
        }
        if (password.length() < 8) {
            showError("Password must be at least 8 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        setLoading(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    createProfileWithBackupCode(uid, name, email);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError("Registration failed: " + e.getMessage());
                });
    }

    private void createProfileWithBackupCode(String uid, String name, String email) {
        try {
            String code = BackupCodeHelper.generateCode();
            String salt = BackupCodeHelper.generateSalt();
            String hash = BackupCodeHelper.hashCode(code, salt);

            Map<String, Object> user = new HashMap<>();
            user.put("uid", uid);
            user.put("displayName", name);
            user.put("email", email);
            user.put("verified", false);
            user.put("admin", false);
            user.put("wipeCodeHash", hash);
            user.put("wipeCodeSalt", salt);

            db.collection("users").document(uid).set(user)
                    .addOnSuccessListener(unused -> {
                        Intent intent = new Intent(this, BackupCodeActivity.class);
                        intent.putExtra("backupCode", code);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        showError("Failed to save profile. Try again.");
                    });

        } catch (Exception e) {
            setLoading(false);
            showError("Could not generate backup code: " + e.getMessage());
        }
    }

    private String text(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!loading);
    }

    private void showError(String message) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
    }
}
