package com.mowtiie.messecure.ui.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.mowtiie.messecure.util.KeystoreHelper;
import com.mowtiie.messecure.util.NetworkUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<Message> messages = new ArrayList<>();

    private TextInputEditText inputField;
    private FloatingActionButton sendButton;

    private FirebaseFirestore db;
    private String currentUid;
    private String convId;
    private String otherUserName;

    private ListenerRegistration messageListener;

    private int selectedTimer = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootView), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


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
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new MessageAdapter(messages, currentUid);
        recyclerView.setAdapter(adapter);

        ChipGroup chipGroup = findViewById(R.id.timerChipGroup);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedTimer = 0;
            } else {
                int chipId = checkedIds.get(0);
                Chip chip = group.findViewById(chipId);
                if (chip != null) {
                    String label = chip.getText().toString();
                    if (label.equals("5 min"))  selectedTimer = 5;
                    else if (label.equals("1 hr")) selectedTimer = 60;
                    else selectedTimer = 0;
                }
            }
            db.collection("conversations").document(convId)
                    .update("destructTimer", selectedTimer);
        });

        inputField = findViewById(R.id.inputField);
        sendButton = findViewById(R.id.sendButton);
        sendButton.setOnClickListener(v -> sendMessage());

        loadCurrentTimer();
        listenForMessages();
        markMessagesAsRead();
    }

    private void loadCurrentTimer() {
        db.collection("conversations").document(convId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains("destructTimer")) {
                        selectedTimer = doc.getLong("destructTimer").intValue();
                    }
                });
    }

    private void listenForMessages() {
        messageListener = db.collection("conversations")
                .document(convId)
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
                            String decrypted = KeystoreHelper.decrypt(msg.getText());
                            msg.setDecryptedText(decrypted);
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

        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "No connection — message will send when back online",
                    Toast.LENGTH_SHORT).show();
        }

        String encryptedText;
        try {
            encryptedText = KeystoreHelper.encrypt(plainText);
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
                .collection("messages")
                .add(messageData)
                .addOnSuccessListener(ref -> {
                    // Update last message preview on the conversation document
                    db.collection("conversations").document(convId).update(
                            "lastMessage", plainText,
                            "lastMessageTime", FieldValue.serverTimestamp());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send. Try again.", Toast.LENGTH_SHORT).show());
    }

    private void markMessagesAsRead() {
        db.collection("conversations")
                .document(convId)
                .collection("messages")
                .whereNotEqualTo("senderId", currentUid)
                .whereEqualTo("readAt", null)
                .get()
                .addOnSuccessListener(snapshots -> {
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        doc.getReference().update("readAt", FieldValue.serverTimestamp());
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) messageListener.remove();
    }
}
