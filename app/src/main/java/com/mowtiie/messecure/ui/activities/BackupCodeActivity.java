package com.mowtiie.messecure.ui.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.mowtiie.messecure.R;

public class BackupCodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_backup_code);

        String code = getIntent().getStringExtra("backupCode");

        TextView codeText = findViewById(R.id.codeText);
        codeText.setText(code != null ? code : "ERROR");

        Button copyButton = findViewById(R.id.copyButton);
        copyButton.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Messecure Backup Code", code));
        });

        Button continueButton = findViewById(R.id.continueButton);
        continueButton.setOnClickListener(v -> {
            startActivity(new Intent(this, BiometricActivity.class));
            finishAffinity();
        });

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(BackupCodeActivity.this, "Please save your backup code, then tap Continue.", Toast.LENGTH_SHORT).show();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }
}