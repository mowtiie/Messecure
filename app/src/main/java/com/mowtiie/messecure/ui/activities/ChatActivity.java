package com.mowtiie.messecure.ui.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import com.mowtiie.messecure.util.LastSeenHelper;
import com.mowtiie.messecure.util.NetworkUtils;
import com.mowtiie.messecure.util.SecurityHelper;
import com.mowtiie.messecure.util.TypingHelper;
import com.mowtiie.messecure.util.UnreadCountHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private MessageAdapter adapter;
    private final List<Message> messages = new ArrayList<>();

    private TextInputEditText inputField;
    private ImageButton sendButton;
    private ChipGroup chipGroup;

    private View replyBar;
    private TextView replyBarSender;
    private TextView replyBarText;
    private ImageButton replyBarClose;
    private Message activeReplyTarget;

    private View newMessagePill;
    private TextView typingIndicator;

    private FirebaseFirestore db;
    private String currentUid;
    private String convId;
    private String otherUserName;
    private String otherUid;

    private String conversationKey;
    private int selectedTimer = 0;

    private ListenerRegistration messageListener;
    private ListenerRegistration convListener;

    private boolean userIsNearBottom = true;

    private boolean hasShownSelfDestructWarning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);
        SecurityHelper.applyScreenshotBlock(this);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getUid();
        convId = getIntent().getStringExtra("convId");
        otherUserName = getIntent().getStringExtra("otherUserName");

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(otherUserName != null ? otherUserName : "Chat");
        }
        findViewById(R.id.toolbar).setOnClickListener(v -> showEncryptionInfoDialog());

        recyclerView = findViewById(R.id.recyclerView);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new MessageAdapter(messages, currentUid, this::showMessageActionsSheet);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@androidx.annotation.NonNull RecyclerView rv, int dx, int dy) {
                int last = layoutManager.findLastVisibleItemPosition();
                userIsNearBottom = (last >= messages.size() - 2);
                if (userIsNearBottom) hideNewMessagePill();
            }
        });

        chipGroup = findViewById(R.id.timerChipGroup);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedTimer = 0;
            } else {
                Chip chip = group.findViewById(checkedIds.get(0));
                if (chip != null) {
                    String label = chip.getText().toString();
                    if (label.equals("5 min"))     selectedTimer = 5;
                    else if (label.equals("1 hr")) selectedTimer = 60;
                    else                           selectedTimer = 0;
                }
            }
            db.collection("conversations").document(convId)
                    .update("destructTimer", selectedTimer);

            if (selectedTimer > 0 && !hasShownSelfDestructWarning) {
                showSelfDestructWarning();
            }
        });

        inputField = findViewById(R.id.inputField);
        sendButton = findViewById(R.id.sendButton);
        sendButton.setOnClickListener(v -> sendMessage());

        inputField.addTextChangedListener(new TextWatcher() {
            private long lastTypingWrite = 0;
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                long now = System.currentTimeMillis();
                if (s.length() > 0 && now - lastTypingWrite > 2000) {
                    TypingHelper.setTyping(convId);
                    lastTypingWrite = now;
                }
            }
        });

        replyBar       = findViewById(R.id.replyBar);
        replyBarSender = findViewById(R.id.replyBarSender);
        replyBarText   = findViewById(R.id.replyBarText);
        replyBarClose  = findViewById(R.id.replyBarClose);
        replyBarClose.setOnClickListener(v -> clearActiveReply());

        newMessagePill = findViewById(R.id.newMessagePill);
        newMessagePill.setOnClickListener(v -> {
            recyclerView.smoothScrollToPosition(messages.size() - 1);
            hideNewMessagePill();
        });

        typingIndicator = findViewById(R.id.typingIndicator);

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

                        List<String> members = (List<String>) doc.get("members");
                        if (members != null) {
                            for (String m : members) {
                                if (!m.equals(currentUid)) { otherUid = m; break; }
                            }
                        }
                    }

                    if (conversationKey == null) {
                        Toast.makeText(this,
                                "Missing conversation key. Messages cannot be decrypted.",
                                Toast.LENGTH_LONG).show();
                    }

                    listenForMessages();
                    listenForConversationMeta();
                    UnreadCountHelper.markAsRead(convId);
                });
    }

    private void restoreTimerChip(int minutes) {
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

                    int oldSize = messages.size();
                    messages.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Message msg = doc.toObject(Message.class);
                        if (msg == null) continue;
                        msg.setId(doc.getId());

                        try {
                            if (conversationKey != null) {
                                msg.setDecryptedText(
                                        ConversationCrypto.decrypt(msg.getText(), conversationKey));
                                if (msg.getReplyToPreview() != null) {
                                    msg.setDecryptedReplyPreview(
                                            ConversationCrypto.decrypt(msg.getReplyToPreview(), conversationKey));
                                }
                            } else {
                                msg.setDecryptedText("[No key]");
                            }
                        } catch (Exception e) {
                            msg.setDecryptedText("[Unable to decrypt]");
                        }
                        messages.add(msg);
                    }

                    adapter.notifyDataSetChanged();

                    boolean newOnesArrived = messages.size() > oldSize;
                    if (newOnesArrived) {
                        if (userIsNearBottom) {
                            recyclerView.scrollToPosition(messages.size() - 1);
                        } else {
                            showNewMessagePill();
                        }
                    }

                    markMessagesAsRead();
                    UnreadCountHelper.markAsRead(convId);
                });
    }

    private void listenForConversationMeta() {
        convListener = db.collection("conversations").document(convId)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null || !snap.exists()) return;

                    Object typingObj = snap.get("typing");
                    if (typingObj instanceof Map && otherUid != null) {
                        Object entry = ((Map<?, ?>) typingObj).get(otherUid);
                        Long lastTyping = (entry instanceof Number) ? ((Number) entry).longValue() : null;
                        if (TypingHelper.isStillTyping(lastTyping)) {
                            typingIndicator.setText(otherUserName + " is typing...");
                            typingIndicator.setVisibility(View.VISIBLE);
                        } else {
                            typingIndicator.setVisibility(View.GONE);
                        }
                    } else {
                        typingIndicator.setVisibility(View.GONE);
                    }
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
        String encryptedReplyPreview = null;
        try {
            encryptedText = ConversationCrypto.encrypt(plainText, conversationKey);
            if (activeReplyTarget != null && activeReplyTarget.getDecryptedText() != null) {
                String preview = activeReplyTarget.getDecryptedText();
                if (preview.length() > 120) preview = preview.substring(0, 120) + "...";
                encryptedReplyPreview = ConversationCrypto.encrypt(preview, conversationKey);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Encryption error. Cannot send.", Toast.LENGTH_SHORT).show();
            return;
        }

        triggerHaptic();

        inputField.setText("");
        TypingHelper.clearTyping(convId);

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId",             currentUid);
        messageData.put("text",                 encryptedText);
        messageData.put("sentAt",               FieldValue.serverTimestamp());
        messageData.put("readAt",               null);
        messageData.put("selfDestruct",         selectedTimer > 0);
        messageData.put("destructAfterMinutes", selectedTimer);

        if (activeReplyTarget != null) {
            messageData.put("replyToId", activeReplyTarget.getId());
            messageData.put("replyToSenderId", activeReplyTarget.getSenderId());
            if (encryptedReplyPreview != null) {
                messageData.put("replyToPreview", encryptedReplyPreview);
            }
        }

        String finalEncryptedText = encryptedText;
        db.collection("conversations").document(convId)
                .collection("messages").add(messageData)
                .addOnSuccessListener(ref -> {
                    db.collection("conversations").document(convId).update(
                            "lastMessage", finalEncryptedText,
                            "lastMessageTime", FieldValue.serverTimestamp());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send.", Toast.LENGTH_SHORT).show());

        clearActiveReply();
    }

    private void triggerHaptic() {
        if (sendButton != null) {
            sendButton.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        } else {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
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

    private void setActiveReply(Message target) {
        activeReplyTarget = target;
        replyBar.setVisibility(View.VISIBLE);
        boolean fromMe = target.isSentByCurrentUser(currentUid);
        replyBarSender.setText(fromMe ? "You" : otherUserName);
        String preview = target.getDecryptedText() != null
                ? target.getDecryptedText() : "(message)";
        if (preview.length() > 80) preview = preview.substring(0, 80) + "...";
        replyBarText.setText(preview);
        inputField.requestFocus();
    }

    private void clearActiveReply() {
        activeReplyTarget = null;
        replyBar.setVisibility(View.GONE);
    }

    private void showMessageActionsSheet(Message msg) {
        if (msg == null) return;

        boolean isMine = msg.isSentByCurrentUser(currentUid);

        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(this)
                .setTitle("Message Actions");

        List<String> options = new ArrayList<>();
        options.add("Reply");
        options.add("Copy text");
        if (isMine) options.add("Delete");

        b.setItems(options.toArray(new String[0]), (d, which) -> {
            String choice = options.get(which);
            switch (choice) {
                case "Reply":
                    setActiveReply(msg);
                    break;
                case "Copy text":
                    if (msg.getDecryptedText() != null) {
                        android.content.ClipboardManager cm = (android.content.ClipboardManager)
                                getSystemService(CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(android.content.ClipData.newPlainText(
                                "Message", msg.getDecryptedText()));
                        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case "Delete":
                    deleteMessage(msg);
                    break;
            }
        });
        b.show();
    }

    private void deleteMessage(Message msg) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete message?")
                .setMessage("This deletes it for everyone.")
                .setPositiveButton("Delete", (d, w) ->
                        db.collection("conversations").document(convId)
                                .collection("messages").document(msg.getId()).delete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEncryptionInfoDialog() {
        String timerText;
        if (selectedTimer == 0)       timerText = "Off";
        else if (selectedTimer == 5)  timerText = "5 minutes after read";
        else if (selectedTimer == 60) timerText = "1 hour after read";
        else                          timerText = selectedTimer + " minutes after read";

        new MaterialAlertDialogBuilder(this)
                .setTitle("\uD83D\uDD12 End-to-End Encryption")
                .setMessage(
                        "Messages in this chat are encrypted with AES-256.\n\n" +
                                "Self-destruct: " + timerText + "\n\n" +
                                "Screenshots are blocked on this screen. Note: the other " +
                                "person can still photograph their screen with another " +
                                "device — treat sensitive content accordingly.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showSelfDestructWarning() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("self_destruct_warning_shown", false)) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Self-Destructing Messages")
                .setMessage(
                        "Messages you send with a timer will be deleted from " +
                                "Firebase " + selectedTimer + " minutes after the recipient " +
                                "reads them.\n\n" +
                                "Important: the recipient can still screenshot or photograph " +
                                "the message before it self-destructs. This is not " +
                                "tamper-proof — only an extra layer.")
                .setPositiveButton("Got it", (d, w) ->
                        prefs.edit().putBoolean("self_destruct_warning_shown", true).apply())
                .setCancelable(false)
                .show();
        hasShownSelfDestructWarning = true;
    }

    private void showNewMessagePill() {
        newMessagePill.setVisibility(View.VISIBLE);
    }

    private void hideNewMessagePill() {
        newMessagePill.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SecurityHelper.applyScreenshotBlock(this);
        LastSeenHelper.touch();
        UnreadCountHelper.markAsRead(convId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        TypingHelper.clearTyping(convId);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) messageListener.remove();
        if (convListener != null)    convListener.remove();
        TypingHelper.clearTyping(convId);
    }
}