package com.secureshare.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.secureshare.app.R;
import com.secureshare.app.databinding.ActivityLoginBinding;
import com.secureshare.app.firebase.AuthManager;
import com.secureshare.app.ui.main.MainActivity;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = AuthManager.getInstance();

        if (authManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        setupAnimations();
        setupListeners();
    }

    private void setupAnimations() {
        binding.cardLogin.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
    }

    private void setupListeners() {
        binding.btnLogin.setOnClickListener(v -> performLogin());
        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        binding.tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void performLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (email.isEmpty()) { binding.etEmail.setError("Email required"); return; }
        if (password.isEmpty()) { binding.etPassword.setError("Password required"); return; }

        showLoading(true);
        authManager.login(email, password)
                .addOnSuccessListener(authResult -> {
                    showLoading(false);
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!show);
        binding.tvForgotPassword.setEnabled(!show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
