package com.mowtiie.messecure.ui.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

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

        biometricSwitch.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean("biometric_enabled", checked).apply());

        screenshotSwitch.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean("screenshot_block", checked).apply());

        stealthNotifSwitch.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean("stealth_notif", checked).apply());

        view.findViewById(R.id.timerRow).setOnClickListener(v -> showTimerPicker());
    }

    private void showTimerPicker() {
        String[] options = {"Off", "5 minutes", "1 hour", "24 hours"};
        int[] values     = {0, 5, 60, 1440};
        int current      = prefs.getInt("default_timer", 0);

        int checkedItem = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) { checkedItem = i; break; }
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Default Self-Destruct Timer")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    prefs.edit().putInt("default_timer", values[which]).apply();
                    dialog.dismiss();
                })
                .show();
    }
}
