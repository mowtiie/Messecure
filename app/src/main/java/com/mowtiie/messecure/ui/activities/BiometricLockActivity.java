package com.mowtiie.messecure.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.mowtiie.messecure.R;
import com.mowtiie.messecure.databinding.ActivityBiometricLockBinding;

import java.util.concurrent.Executor;

public class BiometricLockActivity extends MessecureActivity {

    private ActivityBiometricLockBinding binding;

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityBiometricLockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        checkBiometricAvailability();

        executor = ContextCompat.getMainExecutor(BiometricLockActivity.this);
        setupBiometricPrompt();

        triggerBiometricAuthentication();

        binding.btnTriggerBiometrics.setOnClickListener(view -> triggerBiometricAuthentication());
    }

    private void checkBiometricAvailability() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG;

        switch (biometricManager.canAuthenticate(authenticators)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(this, "Security Hardware missing from this device.", Toast.LENGTH_LONG).show();
                bypassForDevelopment();
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(this, "Security hardware is currently busy or unavailable.", Toast.LENGTH_LONG).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(this, "No fingerprint signatures registered. Please setup your system security keys.", Toast.LENGTH_LONG).show();
                break;
        }
    }

    private void setupBiometricPrompt() {
        biometricPrompt = new BiometricPrompt(BiometricLockActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(), "Authentication required: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                navigateToChatsFeed();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Identity verification rejected. Try again.", Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Messecure Authorization")
                .setSubtitle("Confirm identity to open secure chats workspace")
                .setNegativeButtonText("Exit App")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();
    }

    private void triggerBiometricAuthentication() {
        if (biometricPrompt != null && promptInfo != null) {
            biometricPrompt.authenticate(promptInfo);
        }
    }

    private void navigateToChatsFeed() {
        Intent intent = new Intent(BiometricLockActivity.this, ChatsActivity.class);
        startActivity(intent);
        finish();
    }

    private void bypassForDevelopment() {
        // Temporary handler to jump lines while compiling inside emulators lacking physical scanners
        // Comment out this method call in real deployment builds!
        navigateToChatsFeed();
    }
}