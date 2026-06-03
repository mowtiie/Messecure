package com.mowtiie.messecure.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.mowtiie.messecure.R;
import com.mowtiie.messecure.util.LastSeenHelper;
import com.mowtiie.messecure.util.RoutingHelper;

import java.util.concurrent.Executor;

public class BiometricActivity extends AppCompatActivity {

    private MaterialButton retryButton;
    private MaterialTextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_biometric);

        retryButton = findViewById(R.id.retryButton);
        statusText  = findViewById(R.id.statusText);
        if (retryButton != null) {
            retryButton.setOnClickListener(v -> showBiometricPrompt());
            retryButton.setVisibility(View.GONE);
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean biometricEnabled = prefs.getBoolean("biometric_enabled", true);
        if (!biometricEnabled) {
            route();
            return;
        }

        BiometricManager bm = BiometricManager.from(this);
        int canAuth = bm.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.BIOMETRIC_WEAK);

        switch (canAuth) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                showBiometricPrompt();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(this,
                        "Biometric unavailable. Continuing.", Toast.LENGTH_SHORT).show();
                route();
                break;
            default:
                route();
                break;
        }
    }

    private void showBiometricPrompt() {
        if (retryButton != null) retryButton.setVisibility(View.GONE);
        if (statusText != null) statusText.setText("Touch sensor to unlock");

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        route();
                    }
                    @Override
                    public void onAuthenticationFailed() {
                        if (statusText != null) statusText.setText("Authentication failed. Try again.");
                    }
                    @Override
                    public void onAuthenticationError(int code, @NonNull CharSequence err) {
                        if (code == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            FirebaseAuth.getInstance().signOut();
                            startActivity(new Intent(BiometricActivity.this, LoginActivity.class));
                            finish();
                        } else {
                            if (retryButton != null) retryButton.setVisibility(View.VISIBLE);
                            if (statusText != null) statusText.setText("Cancelled. Tap Try Again to retry.");
                        }
                    }
                });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Messecure")
                .setSubtitle("Verify your identity to continue")
                .setNegativeButtonText("Use Password")
                .build();
        prompt.authenticate(info);
    }

    private void route() {
        LastSeenHelper.touch();
        RoutingHelper.resolveDestination(this, intent -> {
            startActivity(intent);
            finish();
        });
    }
}
