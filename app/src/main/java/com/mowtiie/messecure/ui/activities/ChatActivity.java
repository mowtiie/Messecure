package com.mowtiie.messecure.ui.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.mowtiie.messecure.R;
import com.mowtiie.messecure.ui.adapters.MessageAdapter;
import com.mowtiie.messecure.data.Message;
import com.mowtiie.messecure.util.ConversationCrypto;
import com.mowtiie.messecure.util.NetworkUtils;
import com.mowtiie.messecure.util.SecurityHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private final List<Message> messages = new ArrayList<>();

    private TextInputEditText inputField;
    private ChipGroup chipGroup;

    private FirebaseFirestore db;
    private String currentUid;
    private String convId;
    private String otherUserName;

    private String conversationKey;   // Base64 shared key, fetched on open
    private int selectedTimer = 0;     // minutes; 0 = off

    private ListenerRegistration messageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        SecurityHelper.applyScreenshotBlock(this);

        db         = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getUid();
        convId     = getIntent().getStringExtra("convId");
        otherUserName = getIntent().getStringExtra("otherUserName");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(otherUserName != null ? otherUserName : "Chat");
        }

        recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recyclerView.setLayoutManager(lm);
        adapter = new MessageAdapter(messages, currentUid);
        recyclerView.setAdapter(adapter);

        chipGroup = findViewById(R.id.timerChipGroup);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedTimer = 0;
            } else {
                Chip chip = group.findViewById(checkedIds.get(0));
                if (chip != null) {
                    String label = chip.getText().toString();
                    if (label.equals("5 min"))      selectedTimer = 5;
                    else if (label.equals("1 hr"))  selectedTimer = 60;
                    else                            selectedTimer = 0;
                }
            }
            // Persist the per-conversation timer choice
            db.collection("conversations").document(convId)
                    .update("destructTimer", selectedTimer);
        });

        inputField = findViewById(R.id.inputField);
        ImageButton sendButton = findViewById(R.id.sendButton);
        sendButton.setOnClickListener(v -> sendMessage());

        // Load the conversation (key + saved timer), THEN start listening
        loadConversationThenListen();
    }

    private void loadConversationThenListen() {
        db.collection("conversations").document(convId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        conversationKey = doc.getString("encryptionKey");

                        Long saved = doc.getLong("destructTimer");
                        if (saved != null && saved > 0) {
                            selectedTimer = saved.intValue();
                        } else {
                            selectedTimer = PreferenceManager
                                    .getDefaultSharedPreferences(this)
                                    .getInt("default_timer", 0);
                        }
                        restoreTimerChip(selectedTimer);
                    }

                    if (conversationKey == null) {
                        Toast.makeText(this,
                                "Missing conversation key. Messages cannot be decrypted.",
                                Toast.LENGTH_LONG).show();
                    }

                    listenForMessages();
                    markMessagesAsRead();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Failed to load conversation: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }

    private void restoreTimerChip(int minutes) {
        // Check the chip that matches the saved timer so the selection persists
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            String label = chip.getText().toString();
            boolean match =
                    (minutes == 0  && label.equals("Off")) ||
                            (minutes == 5  && label.equals("5 min")) ||
                            (minutes == 60 && label.equals("1 hr"));
            chip.setChecked(match);
        }
    }

    private void listenForMessages() {
        messageListener = db.collection("conversations").document(convId)
                .collection("messages")
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    messages.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Message msg = doc.toObject(Message.class);
                        if (msg == null) continue;
                        msg.setId(doc.getId());

                        try {
                            if (conversationKey != null) {
                                msg.setDecryptedText(
                                        ConversationCrypto.decrypt(msg.getText(), conversationKey));
                            } else {
                                msg.setDecryptedText("[No key]");
                            }
                        } catch (Exception e) {
                            msg.setDecryptedText("[Unable to decrypt]");
                        }
                        messages.add(msg);
                    }

                    adapter.notifyDataSetChanged();
                    if (!messages.isEmpty()) {
                        recyclerView.scrollToPosition(messages.size() - 1);
                    }
                    markMessagesAsRead();
                });
    }

    private void sendMessage() {
        if (inputField.getText() == null) return;
        String plainText = inputField.getText().toString().trim();
        if (plainText.isEmpty()) return;

        if (conversationKey == null) {
            Toast.makeText(this, "Cannot send — missing encryption key.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "No connection — will send when back online", Toast.LENGTH_SHORT).show();
        }

        String encryptedText;
        String encryptedPreview;
        try {
            encryptedText    = ConversationCrypto.encrypt(plainText, conversationKey);
            encryptedPreview = encryptedText; // same ciphertext reused for preview
        } catch (Exception e) {
            Toast.makeText(this, "Encryption error. Cannot send.", Toast.LENGTH_SHORT).show();
            return;
        }

        inputField.setText("");

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId",             currentUid);
        messageData.put("text",                 encryptedText);
        messageData.put("sentAt",               FieldValue.serverTimestamp());
        messageData.put("readAt",               null);
        messageData.put("selfDestruct",         selectedTimer > 0);
        messageData.put("destructAfterMinutes", selectedTimer);

        db.collection("conversations").document(convId)
                .collection("messages").add(messageData)
                .addOnSuccessListener(ref ->
                        db.collection("conversations").document(convId).update(
                                "lastMessage", encryptedPreview,
                                "lastMessageTime", FieldValue.serverTimestamp()))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send.", Toast.LENGTH_SHORT).show());
    }

    private void markMessagesAsRead() {
        db.collection("conversations").document(convId)
                .collection("messages")
                .whereNotEqualTo("senderId", currentUid)
                .whereEqualTo("readAt", null)
                .get()
                .addOnSuccessListener(snaps -> {
                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        doc.getReference().update("readAt", FieldValue.serverTimestamp());
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SecurityHelper.applyScreenshotBlock(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) messageListener.remove();
    }
}