package com.mowtiie.messecure.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

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

    private RecyclerView recyclerView;
    private ContactsAdapter adapter;
    private List<User> allUsers    = new ArrayList<>();
    private List<User> filtered    = new ArrayList<>();
    private ProgressBar progressBar;

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

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ContactsAdapter(filtered, user -> openOrCreateConversation(user));
        recyclerView.setAdapter(adapter);

        SearchView searchView = view.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterUsers(newText);
                return true;
            }
        });

        loadVerifiedUsers();
    }

    private void loadVerifiedUsers() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users")
                .whereEqualTo("verified", true)
                .get()
                .addOnSuccessListener(snapshots -> {
                    progressBar.setVisibility(View.GONE);
                    allUsers.clear();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        if (doc.getId().equals(currentUid)) continue;
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setUid(doc.getId());
                            allUsers.add(user);
                        }
                    }

                    filtered.clear();
                    filtered.addAll(allUsers);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> progressBar.setVisibility(View.GONE));
    }

    private void filterUsers(String query) {
        filtered.clear();
        if (query == null || query.trim().isEmpty()) {
            filtered.addAll(allUsers);
        } else {
            String lower = query.toLowerCase().trim();
            for (User user : allUsers) {
                if ((user.getDisplayName() != null && user.getDisplayName().toLowerCase().contains(lower))
                        || (user.getEmail() != null && user.getEmail().toLowerCase().contains(lower))) {
                    filtered.add(user);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    /**
     * Checks if a conversation already exists between currentUid and otherUid.
     * If yes, opens it. If not, creates a new one first.
     */
    private void openOrCreateConversation(User otherUser) {
        db.collection("conversations")
                .whereArrayContains("members", currentUid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    String existingConvId = null;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
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
                });
    }

    private void createConversation(User otherUser) {
        Map<String, Object> conv = new HashMap<>();
        conv.put("members",         Arrays.asList(currentUid, otherUser.getUid()));
        conv.put("lastMessage",     "");
        conv.put("lastMessageTime", FieldValue.serverTimestamp());
        conv.put("destructTimer",   0);

        db.collection("conversations").add(conv)
                .addOnSuccessListener(ref ->
                        openChat(ref.getId(), otherUser.getDisplayName()));
    }

    private void openChat(String convId, String otherUserName) {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra("convId", convId);
        intent.putExtra("otherUserName", otherUserName);
        startActivity(intent);
    }
}
