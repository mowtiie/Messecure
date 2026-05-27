package com.mowtiie.messecure.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.mowtiie.messecure.R;

public class MainActivity extends AppCompatActivity {

    private NavController navController;

    private long backgroundedAt = -1;
    private static final long LOCK_TIMEOUT_MS = 30_000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.navHostFragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (navController == null) return super.onOptionsItemSelected(item);

        int id = item.getItemId();
        if (id == R.id.menu_contacts) {
            navController.navigate(R.id.contactsFragment);
        } else if (id == R.id.menu_profile) {
            navController.navigate(R.id.profileFragment);
        } else if (id == R.id.menu_settings) {
            navController.navigate(R.id.settingsFragment);
        } else if (id == R.id.menu_wipe) {
            navController.navigate(R.id.wipeFragment);
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
