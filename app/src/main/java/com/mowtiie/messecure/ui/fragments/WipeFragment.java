package com.mowtiie.messecure.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.functions.FirebaseFunctionsException;
import com.mowtiie.messecure.R;
import com.mowtiie.messecure.ui.activities.LoginActivity;

import java.util.HashMap;
import java.util.Map;

public class WipeFragment extends Fragment {

    private static final String TAG = "WipeFragment";

    private static final String FUNCTIONS_REGION = "asia-southeast1";

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
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(requireContext(),
                    "You must be signed in to wipe data.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String mode = wipeScope.getCheckedRadioButtonId() == R.id.radioFull
                ? "full" : "messages";

        setLoading(true);

        Map<String, Object> data = new HashMap<>();
        data.put("mode", mode);

        Log.d(TAG, "Calling remoteWipe in region " + FUNCTIONS_REGION + " with mode=" + mode);

        FirebaseFunctions.getInstance(FUNCTIONS_REGION)
                .getHttpsCallable("remoteWipe")
                .call(data)
                .addOnSuccessListener(result -> {
                    setLoading(false);
                    Log.d(TAG, "Remote wipe succeeded");
                    FirebaseAuth.getInstance().signOut();
                    Toast.makeText(requireContext(),
                            "Wipe complete. All data has been deleted.",
                            Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String detail = describeError(e);
                    Log.e(TAG, "Remote wipe failed: " + detail, e);
                    Toast.makeText(requireContext(),
                            "Wipe failed: " + detail,
                            Toast.LENGTH_LONG).show();
                });
    }

    private String describeError(Exception e) {
        if (e instanceof FirebaseFunctionsException) {
            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
            FirebaseFunctionsException.Code code = ffe.getCode();

            switch (code) {
                case UNAUTHENTICATED:
                    return "Not signed in. Please log in again.";
                case PERMISSION_DENIED:
                    return "Permission denied. Check Firestore security rules.";
                case NOT_FOUND:
                    return "Function not found. Check that it is deployed.";
                case UNAVAILABLE:
                    return "Network unavailable. Try again when online.";
                case INTERNAL:
                    return "Server error. Check Firebase Functions logs for details.";
                case DEADLINE_EXCEEDED:
                    return "Wipe took too long. Try again — the wipe may have partially completed.";
                default:
                    return code.name() + ": " + ffe.getMessage();
            }
        }
        return e.getMessage() != null ? e.getMessage() : "Unknown error";
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        wipeButton.setEnabled(!loading);
    }
}
