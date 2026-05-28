package com.mowtiie.messecure.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.mowtiie.messecure.R;

import java.util.HashMap;
import java.util.Map;

public class WipeActivity extends AppCompatActivity {

    private static final String TAG = "WipeFragment";

    private static final String FUNCTIONS_REGION = "us-central1";

    private RadioGroup wipeScope;
    private Button wipeButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_wipe);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        wipeScope   = findViewById(R.id.wipeScope);
        wipeButton  = findViewById(R.id.wipeButton);
        progressBar = findViewById(R.id.progressBar);

        wipeButton.setOnClickListener(v -> confirmWipe());
    }

    private void confirmWipe() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Remote Wipe")
                .setMessage("This is irreversible. All selected data will be permanently deleted from Firebase. Are you sure?")
                .setPositiveButton("Wipe Now", (dialog, which) -> executeWipe())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeWipe() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this,
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
                    Toast.makeText(this,
                            "Wipe complete. All data has been deleted.",
                            Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String detail = describeError(e);
                    Log.e(TAG, "Remote wipe failed: " + detail, e);
                    Toast.makeText(this,
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