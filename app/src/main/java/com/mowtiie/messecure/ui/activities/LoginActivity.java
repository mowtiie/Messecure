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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mowtiie.messecure.R;
import com.mowtiie.messecure.util.NotificationPermissionHelper;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailField, passwordField;
    private Button loginButton;
    private ProgressBar progressBar;
    private View rootView;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootView), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        rootView      = findViewById(R.id.rootView);
        emailField    = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        loginButton   = findViewById(R.id.loginButton);
        progressBar   = findViewById(R.id.progressBar);

        loginButton.setOnClickListener(v -> attemptLogin());

        TextView registerLink = findViewById(R.id.registerLink);
        registerLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        TextView emergencyWipeLink = findViewById(R.id.emergencyWipeLink);
        emergencyWipeLink.setOnClickListener(v ->
                startActivity(new Intent(this, EmergencyWipeActivity.class)));

        NotificationPermissionHelper.requestIfNeeded(this);
    }

    private void attemptLogin() {
        String email    = emailField.getText() != null ? emailField.getText().toString().trim() : "";
        String password = passwordField.getText() != null ? passwordField.getText().toString() : "";

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        setLoading(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    saveFcmToken();
                    startActivity(new Intent(this, BiometricActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        showError("Wrong email or password.");
                    } else if (e instanceof FirebaseAuthInvalidUserException) {
                        showError("No account found with this email.");
                    } else {
                        showError("Login failed. Check your connection.");
                    }
                });
    }

    private void saveFcmToken() {
        String uid = auth.getUid();
        if (uid == null) return;
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token ->
                db.collection("users").document(uid).update("fcmToken", token));
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!loading);
    }

    private void showError(String message) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
    }
}
