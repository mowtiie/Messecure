package com.mowtiie.messecure.ui.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.mowtiie.messecure.R;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        SwitchMaterial biometricSwitch    = findViewById(R.id.biometricSwitch);
        SwitchMaterial screenshotSwitch   = findViewById(R.id.screenshotSwitch);
        SwitchMaterial stealthNotifSwitch = findViewById(R.id.stealthNotifSwitch);

        biometricSwitch.setChecked(prefs.getBoolean("biometric_enabled", true));
        screenshotSwitch.setChecked(prefs.getBoolean("screenshot_block", true));
        stealthNotifSwitch.setChecked(prefs.getBoolean("stealth_notif", true));

        biometricSwitch.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("biometric_enabled", checked).apply();
            if (!checked) {
                Toast.makeText(this,
                        "Biometric lock disabled. Takes effect next time you open the app.",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this,
                        "Biometric lock enabled.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        screenshotSwitch.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("screenshot_block", checked).apply();
            applyScreenshotBlock(checked);
            Toast.makeText(this,
                    checked
                            ? "Screenshot block enabled."
                            : "Screenshot block disabled. Other screens will update on next open.",
                    Toast.LENGTH_SHORT).show();
        });

        stealthNotifSwitch.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("stealth_notif", checked).apply();
            Toast.makeText(this,
                    checked
                            ? "Notifications will hide message previews."
                            : "Notifications will show sender names.",
                    Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.timerRow).setOnClickListener(v -> showTimerPicker());
    }

    private void applyScreenshotBlock(boolean enabled) {
        if (getWindow() == null) return;
        if (enabled) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
            );
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    private void showTimerPicker() {
        String[] options = {"Off", "5 minutes", "1 hour", "24 hours"};
        int[] values     = {0, 5, 60, 1440};
        int current      = prefs.getInt("default_timer", 0);

        int checkedItem = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) { checkedItem = i; break; }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Default Self-Destruct Timer")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    prefs.edit().putInt("default_timer", values[which]).apply();
                    Toast.makeText(this,
                            "Default timer set to " + options[which],
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .show();
    }
}