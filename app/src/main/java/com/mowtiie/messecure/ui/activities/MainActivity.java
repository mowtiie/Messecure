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

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.mowtiie.messecure.R;
import com.mowtiie.messecure.data.Conversation;
import com.mowtiie.messecure.ui.adapters.ConversationAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private long backgroundedAt = -1;
    private static final long LOCK_TIMEOUT_MS = 30_000;

    private RecyclerView recyclerView;
    private ConversationAdapter adapter;
    private List<Conversation> conversations = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView emptyView;

    private FirebaseFirestore db;
    private String currentUid;
    private ListenerRegistration listener;

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

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        db         = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getUid();

        recyclerView = findViewById(R.id.recyclerView);
        progressBar  = findViewById(R.id.progressBar);
        emptyView    = findViewById(R.id.emptyView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConversationAdapter(conversations, conversation -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("convId", conversation.getId());
            intent.putExtra("otherUserName", conversation.getOtherUserName());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent chatIntent = new Intent(MainActivity.this, ContactsActivity.class);
            startActivity(chatIntent);
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
            // move to settings activity
        } else if (id == R.id.menu_wipe) {
            Intent wipeIntent = new Intent(MainActivity.this, WipeActivity.class);
            startActivity(wipeIntent);
        } else if (id == R.id.menu_logout) {
            signOut();
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
