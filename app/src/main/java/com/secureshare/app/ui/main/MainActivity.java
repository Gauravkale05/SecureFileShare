package com.secureshare.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.secureshare.app.R;
import com.secureshare.app.databinding.ActivityMainBinding;
import com.secureshare.app.firebase.AuthManager;
import com.secureshare.app.ui.auth.LoginActivity;
import com.secureshare.app.ui.download.DownloadActivity;
import com.secureshare.app.ui.history.HistoryActivity;
import com.secureshare.app.ui.upload.UploadActivity;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = AuthManager.getInstance();
        setupHeader();
        setupCards();
    }

    private void setupHeader() {
        if (authManager.getCurrentUser() == null) {
            binding.tvWelcome.setText("Welcome!");
            return;
        }

        String name = authManager.getCurrentUser().getDisplayName();
        binding.tvWelcome.setText(name != null && !name.isEmpty() ? "Welcome, " + name + "!" : "Welcome!");
    }

    private void setupCards() {
        // Staggered fade-in animation
        binding.cardUpload.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        binding.cardDownload.postDelayed(() ->
                binding.cardDownload.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in)), 100);
        binding.cardHistory.postDelayed(() ->
                binding.cardHistory.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in)), 200);
        binding.cardLogout.postDelayed(() ->
                binding.cardLogout.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in)), 300);

        binding.cardUpload.setOnClickListener(v -> {
            startActivity(new Intent(this, UploadActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        binding.cardDownload.setOnClickListener(v -> {
            startActivity(new Intent(this, DownloadActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        binding.cardHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        binding.cardLogout.setOnClickListener(v -> {
            authManager.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
