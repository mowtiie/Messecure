package com.mowtiie.messecure.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mowtiie.messecure.R;
import com.mowtiie.messecure.data.User;

public class RegisterActivity extends AppCompatActivity {

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
        String name     = nameField.getText() != null ? nameField.getText().toString().trim() : "";
        String email    = emailField.getText() != null ? emailField.getText().toString().trim() : "";
        String password = passwordField.getText() != null ? passwordField.getText().toString() : "";
        String confirm  = confirmPasswordField.getText() != null ? confirmPasswordField.getText().toString() : "";

        // Validate fields
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        // Enforce STI email only
        if (!email.endsWith("@sti.edu.ph")) {
            showError("Only @sti.edu.ph email addresses are allowed.");
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
                    saveUserToFirestore(uid, name, email);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError("Registration failed: " + e.getMessage());
                });
    }

    private void saveUserToFirestore(String uid, String name, String email) {
        User user = new User(uid, name, email);

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(unused -> {
                    startActivity(new Intent(this, BiometricActivity.class));
                    finishAffinity(); // clear back stack
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError("Failed to save profile. Try again.");
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!loading);
    }

    private void showError(String message) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
    }
}
