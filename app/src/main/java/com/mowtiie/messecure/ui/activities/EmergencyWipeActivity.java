package com.mowtiie.messecure.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.mowtiie.messecure.R;

import java.util.HashMap;
import java.util.Map;

public class EmergencyWipeActivity extends AppCompatActivity {

    private static final String TAG    = "EmergencyWipe";
    private static final String REGION = "asia-southeast1";

    private TextInputEditText emailInput;
    private TextInputEditText codeInput;
    private RadioGroup wipeScope;
    private Button            wipeButton;
    private Button cancelButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_emergency_wipe);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        emailInput   = findViewById(R.id.emailInput);
        codeInput    = findViewById(R.id.codeInput);
        wipeScope    = findViewById(R.id.wipeScope);
        wipeButton   = findViewById(R.id.wipeButton);
        cancelButton = findViewById(R.id.cancelButton);
        progressBar  = findViewById(R.id.progressBar);

        wipeButton.setOnClickListener(v -> confirmWipe());
        cancelButton.setOnClickListener(v -> finish());
    }

    private void confirmWipe() {
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String code  = codeInput.getText()  != null ? codeInput.getText().toString().trim()  : "";

        if (email.isEmpty() || code.isEmpty()) {
            Toast.makeText(this, "Email and backup code are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Emergency Wipe")
                .setMessage("This will permanently delete the data for " + email +
                        ". This cannot be undone. Continue?")
                .setPositiveButton("Wipe Now", (d, w) -> executeWipe(email, code))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeWipe(String email, String code) {
        String mode = wipeScope.getCheckedRadioButtonId() == R.id.radioFull ? "full" : "messages";

        setLoading(true);

        Map<String, Object> data = new HashMap<>();
        data.put("email",      email);
        data.put("backupCode", code);
        data.put("mode",       mode);

        FirebaseFunctions.getInstance(REGION)
                .getHttpsCallable("emergencyWipe")
                .call(data)
                .addOnSuccessListener(result -> {
                    setLoading(false);
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Wipe Complete")
                            .setMessage("All data for " + email + " has been deleted.")
                            .setPositiveButton("OK", (d, w) -> finish())
                            .setCancelable(false)
                            .show();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String detail = describeError(e);
                    Log.e(TAG, "emergencyWipe failed: " + detail, e);
                    Toast.makeText(this, "Wipe failed: " + detail, Toast.LENGTH_LONG).show();
                });
    }

    private String describeError(Exception e) {
        if (e instanceof FirebaseFunctionsException) {
            FirebaseFunctionsException.Code code = ((FirebaseFunctionsException) e).getCode();
            switch (code) {
                case PERMISSION_DENIED: return "Invalid email or backup code.";
                case INVALID_ARGUMENT:  return "Email and backup code are required.";
                case UNAVAILABLE:       return "Network unavailable. Try again.";
                case INTERNAL:          return "Server error. Try again later.";
                default:                return code.name();
            }
        }
        return e.getMessage() != null ? e.getMessage() : "Unknown error";
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        wipeButton.setEnabled(!loading);
        cancelButton.setEnabled(!loading);
    }
}