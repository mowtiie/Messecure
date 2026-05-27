package com.mowtiie.messecure.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import com.mowtiie.messecure.R;
import com.mowtiie.messecure.ui.activities.LoginActivity;

import java.util.HashMap;
import java.util.Map;

public class WipeFragment extends Fragment {

    private RadioGroup wipeScope;
    private Button wipeButton;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wipe, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        wipeScope   = view.findViewById(R.id.wipeScope);
        wipeButton  = view.findViewById(R.id.wipeButton);
        progressBar = view.findViewById(R.id.progressBar);

        wipeButton.setOnClickListener(v -> confirmWipe());
    }

    private void confirmWipe() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirm Remote Wipe")
                .setMessage("This is irreversible. All selected data will be permanently deleted from Firebase. Are you sure?")
                .setPositiveButton("Wipe Now", (dialog, which) -> executeWipe())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeWipe() {
        String mode = wipeScope.getCheckedRadioButtonId() == R.id.radioFull
                ? "full" : "messages";

        setLoading(true);

        Map<String, Object> data = new HashMap<>();
        data.put("mode", mode);

        FirebaseFunctions.getInstance()
                .getHttpsCallable("remoteWipe")
                .call(data)
                .addOnSuccessListener(result -> {
                    setLoading(false);
                    FirebaseAuth.getInstance().signOut();
                    Toast.makeText(requireContext(),
                            "Wipe complete. All data has been deleted.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(requireContext(),
                            "Wipe failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        wipeButton.setEnabled(!loading);
    }
}
