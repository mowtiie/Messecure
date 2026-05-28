package com.mowtiie.messecure.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mowtiie.messecure.R;
import com.mowtiie.messecure.ui.activities.ChatActivity;
import com.mowtiie.messecure.ui.adapters.ContactsAdapter;
import com.mowtiie.messecure.data.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactsFragment extends Fragment {

    private static final String TAG = "ContactsFragment";

    private RecyclerView recyclerView;
    private ContactsAdapter adapter;
    private final List<User> allUsers = new ArrayList<>();
    private final List<User> filtered = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView emptyView;

    private FirebaseFirestore db;
    private String currentUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db         = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getUid();

        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar  = view.findViewById(R.id.progressBar);
        emptyView    = view.findViewById(R.id.emptyView);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ContactsAdapter(filtered, this::openOrCreateConversation);
        recyclerView.setAdapter(adapter);

        SearchView searchView = view.findViewById(R.id.searchView);
        searchView.setIconifiedByDefault(false);
        searchView.clearFocus();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterUsers(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterUsers(newText);
                return true;
            }
        });

        loadAllUsers();
    }

    private void loadAllUsers() {
        progressBar.setVisibility(View.VISIBLE);
        if (emptyView != null) emptyView.setVisibility(View.GONE);

        db.collection("users")
                .get()
                .addOnSuccessListener(snapshots -> {
                    progressBar.setVisibility(View.GONE);
                    allUsers.clear();

                    Log.d(TAG, "Firestore returned " + snapshots.size() + " user docs");

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        // Exclude the current user from the contacts list
                        if (doc.getId().equals(currentUid)) continue;

                        User user = doc.toObject(User.class);
                        if (user == null) {
                            Log.w(TAG, "Skipping user doc " + doc.getId() + " — could not deserialize");
                            continue;
                        }

                        user.setUid(doc.getId());
                        allUsers.add(user);
                    }

                    Log.d(TAG, "Loaded " + allUsers.size() + " contacts (excluding current user)");

                    filtered.clear();
                    filtered.addAll(allUsers);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Failed to load users", e);
                    Toast.makeText(requireContext(),
                            "Failed to load contacts: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void filterUsers(String query) {
        filtered.clear();

        if (query == null || query.trim().isEmpty()) {
            filtered.addAll(allUsers);
        } else {
            String lower = query.toLowerCase().trim();
            for (User user : allUsers) {
                String name  = user.getDisplayName() != null ? user.getDisplayName().toLowerCase() : "";
                String email = user.getEmail()       != null ? user.getEmail().toLowerCase()       : "";
                if (name.contains(lower) || email.contains(lower)) {
                    filtered.add(user);
                }
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (emptyView != null) {
            emptyView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void openOrCreateConversation(User otherUser) {
        db.collection("conversations")
                .whereArrayContains("members", currentUid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    String existingConvId = null;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        @SuppressWarnings("unchecked")
                        List<String> members = (List<String>) doc.get("members");
                        if (members != null && members.contains(otherUser.getUid())) {
                            existingConvId = doc.getId();
                            break;
                        }
                    }

                    if (existingConvId != null) {
                        openChat(existingConvId, otherUser.getDisplayName());
                    } else {
                        createConversation(otherUser);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "Could not open chat: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void createConversation(User otherUser) {
        Map<String, Object> conv = new HashMap<>();
        conv.put("members",         Arrays.asList(currentUid, otherUser.getUid()));
        conv.put("lastMessage",     "");
        conv.put("lastMessageTime", FieldValue.serverTimestamp());
        conv.put("destructTimer",   0);

        db.collection("conversations").add(conv)
                .addOnSuccessListener(ref -> openChat(ref.getId(), otherUser.getDisplayName()))
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "Failed to start chat: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void openChat(String convId, String otherUserName) {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra("convId", convId);
        intent.putExtra("otherUserName", otherUserName);
        startActivity(intent);
    }
}
