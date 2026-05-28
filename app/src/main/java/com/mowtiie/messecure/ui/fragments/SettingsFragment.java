package com.mowtiie.messecure.ui.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.mowtiie.messecure.R;

public class SettingsFragment extends Fragment {

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        SwitchMaterial biometricSwitch    = view.findViewById(R.id.biometricSwitch);
        SwitchMaterial screenshotSwitch   = view.findViewById(R.id.screenshotSwitch);
        SwitchMaterial stealthNotifSwitch = view.findViewById(R.id.stealthNotifSwitch);

        biometricSwitch.setChecked(prefs.getBoolean("biometric_enabled", true));
        screenshotSwitch.setChecked(prefs.getBoolean("screenshot_block", true));
        stealthNotifSwitch.setChecked(prefs.getBoolean("stealth_notif", true));

        biometricSwitch.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("biometric_enabled", checked).apply();
            if (!checked) {
                Toast.makeText(requireContext(),
                        "Biometric lock disabled. Takes effect next time you open the app.",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(requireContext(),
                        "Biometric lock enabled.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        screenshotSwitch.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("screenshot_block", checked).apply();
            applyScreenshotBlock(checked);
            Toast.makeText(requireContext(),
                    checked
                            ? "Screenshot block enabled."
                            : "Screenshot block disabled. Other screens will update on next open.",
                    Toast.LENGTH_SHORT).show();
        });

        stealthNotifSwitch.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("stealth_notif", checked).apply();
            Toast.makeText(requireContext(),
                    checked
                            ? "Notifications will hide message previews."
                            : "Notifications will show sender names.",
                    Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.timerRow).setOnClickListener(v -> showTimerPicker());
    }

    private void applyScreenshotBlock(boolean enabled) {
        if (getActivity() == null) return;
        if (enabled) {
            getActivity().getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
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

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Default Self-Destruct Timer")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    prefs.edit().putInt("default_timer", values[which]).apply();
                    Toast.makeText(requireContext(),
                            "Default timer set to " + options[which],
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .show();
    }
}
