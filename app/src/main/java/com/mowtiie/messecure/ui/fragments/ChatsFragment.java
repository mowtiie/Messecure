package com.mowtiie.messecure.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.mowtiie.messecure.R;
import com.mowtiie.messecure.ui.activities.ChatActivity;
import com.mowtiie.messecure.ui.adapters.ConversationAdapter;
import com.mowtiie.messecure.data.Conversation;

import java.util.ArrayList;
import java.util.List;

public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ConversationAdapter adapter;
    private List<Conversation> conversations = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView emptyView;

    private FirebaseFirestore db;
    private String currentUid;
    private ListenerRegistration listener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chats, container, false);
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
        adapter = new ConversationAdapter(conversations, conversation -> {
            Intent intent = new Intent(requireContext(), ChatActivity.class);
            intent.putExtra("convId", conversation.getId());
            intent.putExtra("otherUserName", conversation.getOtherUserName());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.navHostFragment, new ContactsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        listenForConversations();
    }

    private void listenForConversations() {
        progressBar.setVisibility(View.VISIBLE);

        listener = db.collection("conversations")
                .whereArrayContains("members", currentUid)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (error != null || snapshots == null) return;

                    conversations.clear();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Conversation conv = doc.toObject(Conversation.class);
                        if (conv == null) continue;
                        conv.setId(doc.getId());

                        String otherUid = conv.getOtherUserId(currentUid);
                        if (otherUid != null) {
                            db.collection("users").document(otherUid).get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            conv.setOtherUserName(userDoc.getString("displayName"));
                                            conv.setOtherUserEmail(userDoc.getString("email"));
                                        }
                                        adapter.notifyDataSetChanged();
                                    });
                        }

                        conversations.add(conv);
                    }

                    emptyView.setVisibility(conversations.isEmpty() ? View.VISIBLE : View.GONE);
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) listener.remove();
    }
}
