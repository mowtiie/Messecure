package com.mowtiie.messecure.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.mowtiie.messecure.R;
import com.mowtiie.messecure.data.User;
import com.mowtiie.messecure.ui.adapters.AdminUserAdapter;
import com.mowtiie.messecure.util.SecurityHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {

    private static final String REGION = "asia-southeast1";

    private final List<User> users = new ArrayList<>();
    private AdminUserAdapter adapter;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseFunctions functions;
    private String myUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);
        SecurityHelper.applyScreenshotBlock(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db        = FirebaseFirestore.getInstance();
        functions = FirebaseFunctions.getInstance(REGION);
        myUid     = FirebaseAuth.getInstance().getUid();

        progressBar = findViewById(R.id.progressBar);
        RecyclerView rv = findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdminUserAdapter(users, myUid, new AdminUserAdapter.AdminActionListener() {
            @Override public void onApprove(User u) { setVerified(u, true); }
            @Override public void onRevoke(User u)  { setVerified(u, false); }
            @Override public void onDelete(User u)  { confirmDelete(u); }
        });
        rv.setAdapter(adapter);

        loadUsers();
    }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("users").get()
                .addOnSuccessListener(snap -> {
                    progressBar.setVisibility(View.GONE);
                    users.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        User u = doc.toObject(User.class);
                        if (u == null) continue;
                        u.setUid(doc.getId());
                        users.add(u);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Failed to load users: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setVerified(User user, boolean verified) {
        Map<String, Object> data = new HashMap<>();
        data.put("targetUid", user.getUid());
        data.put("verified", verified);

        progressBar.setVisibility(View.VISIBLE);
        functions.getHttpsCallable("setVerified").call(data)
                .addOnSuccessListener(r -> {
                    Toast.makeText(this,
                            (verified ? "Approved " : "Revoked ") + user.getDisplayName(),
                            Toast.LENGTH_SHORT).show();
                    loadUsers();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Action failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void confirmDelete(User user) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete " + user.getDisplayName() + "?")
                .setMessage("This permanently deletes their account and wipes all their messages. This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> deleteUser(user))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUser(User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("targetUid", user.getUid());

        progressBar.setVisibility(View.VISIBLE);
        functions.getHttpsCallable("deleteUserAccount").call(data)
                .addOnSuccessListener(r -> {
                    Toast.makeText(this,
                            "Deleted " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                    loadUsers();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}