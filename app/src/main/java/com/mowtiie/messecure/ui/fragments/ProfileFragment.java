package com.mowtiie.messecure.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mowtiie.messecure.R;

public class ProfileFragment extends Fragment {

    private TextView nameText, emailText, avatarLabel;
    private FirebaseFirestore db;
    private String currentUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db         = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getUid();

        nameText    = view.findViewById(R.id.nameText);
        emailText   = view.findViewById(R.id.emailText);
        avatarLabel = view.findViewById(R.id.avatarLabel);

        loadProfile();

        view.findViewById(R.id.editNameButton).setOnClickListener(v -> showEditNameDialog());
    }

    private void loadProfile() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    String name  = doc.getString("displayName");
                    String email = doc.getString("email");

                    nameText.setText(name != null ? name : "");
                    emailText.setText(email != null ? email : "");

                    if (name != null && !name.isEmpty()) {
                        String[] parts = name.trim().split(" ");
                        String initials = parts.length >= 2
                                ? String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)
                                : String.valueOf(parts[0].charAt(0));
                        avatarLabel.setText(initials.toUpperCase());
                    }
                });
    }

    private void showEditNameDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_name, null);
        TextInputEditText nameInput = dialogView.findViewById(R.id.nameInput);
        nameInput.setText(nameText.getText());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Display Name")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = nameInput.getText() != null
                            ? nameInput.getText().toString().trim() : "";
                    if (!newName.isEmpty()) {
                        db.collection("users").document(currentUid)
                                .update("displayName", newName)
                                .addOnSuccessListener(unused -> loadProfile());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
