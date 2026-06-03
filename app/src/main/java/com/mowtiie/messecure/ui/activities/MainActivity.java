package com.mowtiie.messecure.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.mowtiie.messecure.R;
import com.mowtiie.messecure.data.Conversation;
import com.mowtiie.messecure.ui.adapters.AdminUserAdapter;
import com.mowtiie.messecure.ui.adapters.ConversationAdapter;
import com.mowtiie.messecure.util.ConversationCrypto;
import com.mowtiie.messecure.util.NicknameHelper;
import com.mowtiie.messecure.util.NotificationPermissionHelper;
import com.mowtiie.messecure.util.UnreadCountHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private long backgroundedAt = -1;
    private static final long LOCK_TIMEOUT_MS = 30_000;
    private boolean isAdmin = false;

    private ConversationAdapter adapter;
    private final List<Conversation> allConversations = new ArrayList<>();
    private final List<Conversation> filtered = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView emptyView;
    private View emptyIllustration;
    private SwipeRefreshLayout swipeRefresh;

    private FirebaseFirestore db;
    private String currentUid;
    private ListenerRegistration listener;

    private final Map<String, Integer> unreadCounts = new HashMap<>();

    private Map<String, String> nicknames = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        isAdmin = getIntent().getBooleanExtra("isAdmin", false);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getUid();

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        emptyIllustration = findViewById(R.id.emptyIllustration);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConversationAdapter(filtered, unreadCounts, conversation -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("convId", conversation.getId());
            intent.putExtra("otherUserName", conversation.getOtherUserName());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(() -> {
            loadOwnNicknamesThenListen();
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent chatIntent = new Intent(MainActivity.this, ChatActivity.class);
            startActivity(chatIntent);
        });

        loadOwnNicknamesThenListen();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem adminItem = menu.findItem(R.id.menu_admin);
        if (adminItem != null) adminItem.setVisible(isAdmin);
        return super.onPrepareOptionsMenu(menu);
    }

    private void loadOwnNicknamesThenListen() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    nicknames.clear();
                    if (doc.exists()) {
                        Object n = doc.get("nicknames");
                        if (n instanceof Map) {
                            for (Map.Entry<?, ?> e : ((Map<?, ?>) n).entrySet()) {
                                nicknames.put(e.getKey().toString(), e.getValue().toString());
                            }
                        }
                    }
                    listenForConversations();
                });
    }

    private void listenForConversations() {
        if (listener != null) listener.remove();
        progressBar.setVisibility(View.VISIBLE);

        listener = db.collection("conversations")
                .whereArrayContains("members", currentUid)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    if (error != null || snapshots == null) return;

                    allConversations.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Conversation conv = doc.toObject(Conversation.class);
                        if (conv == null) continue;
                        conv.setId(doc.getId());

                        String key = doc.getString("encryptionKey");
                        String cipherText = conv.getLastMessage();
                        if (key != null && cipherText != null && !cipherText.isEmpty()) {
                            try {
                                conv.setLastMessage(ConversationCrypto.decrypt(cipherText, key));
                            } catch (Exception e) {
                                conv.setLastMessage("\uD83D\uDD12 Encrypted message");
                            }
                        }

                        String otherUid = conv.resolveOtherUserId(currentUid);
                        if (otherUid != null) {
                            String nick = nicknames.get(otherUid);
                            if (nick != null) {
                                conv.setOtherUserName(nick);
                            } else {
                                db.collection("users").document(otherUid).get()
                                        .addOnSuccessListener(userDoc -> {
                                            if (userDoc.exists()) {
                                                conv.setOtherUserName(
                                                        NicknameHelper.resolveLabel(
                                                                nicknames.get(otherUid),
                                                                userDoc.getString("displayName")));
                                            }
                                            adapter.notifyDataSetChanged();
                                        });
                            }
                        }

                        allConversations.add(conv);
                        computeUnread(conv.getId());
                    }

                    applyFilter("");
                    updateEmptyState();
                });
    }

    private void computeUnread(String convId) {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(userDoc -> {
                    long lastRead = UnreadCountHelper.getLastRead(userDoc.getData(), convId);
                    UnreadCountHelper.queryUnread(convId, lastRead)
                            .addOnSuccessListener(snap -> {
                                unreadCounts.put(convId, snap.size());
                                adapter.notifyDataSetChanged();
                            });
                });
    }

    private void filter(String query) {
        applyFilter(query);
        updateEmptyState();
    }

    private void applyFilter(String query) {
        filtered.clear();
        if (query == null || query.trim().isEmpty()) {
            filtered.addAll(allConversations);
        } else {
            String lower = query.toLowerCase().trim();
            for (Conversation c : allConversations) {
                String name    = c.getOtherUserName() != null ? c.getOtherUserName().toLowerCase() : "";
                String preview = c.getLastMessage()   != null ? c.getLastMessage().toLowerCase()   : "";
                if (name.contains(lower) || preview.contains(lower)) {
                    filtered.add(c);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void updateEmptyState() {
        boolean isEmpty = filtered.isEmpty();
        if (emptyIllustration != null) {
            emptyIllustration.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        assert searchView != null;
        searchView.setQueryHint("Search here");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_contacts) {
            Intent contactsIntent = new Intent(MainActivity.this, ContactsActivity.class);
            startActivity(contactsIntent);
        } else if (id == R.id.menu_profile) {
            Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(profileIntent);
        } else if (id == R.id.menu_settings) {
            Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
        } else if (id == R.id.menu_wipe) {
            Intent wipeIntent = new Intent(MainActivity.this, WipeActivity.class);
            startActivity(wipeIntent);
        } else if (id == R.id.menu_logout) {
            signOut();
        } else if (id == R.id.menu_admin) {
            Intent adminIntent = new Intent(MainActivity.this, AdminActivity.class);
            startActivity(adminIntent);
        }
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        backgroundedAt = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (backgroundedAt != -1) {
            long elapsed = System.currentTimeMillis() - backgroundedAt;
            if (elapsed > LOCK_TIMEOUT_MS) {
                startActivity(new Intent(this, BiometricActivity.class));
                finish();
                return;
            }
        }
        backgroundedAt = -1;
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
