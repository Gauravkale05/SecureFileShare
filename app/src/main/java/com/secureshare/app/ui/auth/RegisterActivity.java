package com.secureshare.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.secureshare.app.R;
import com.secureshare.app.firebase.AuthManager;
import com.secureshare.app.ui.main.MainActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authManager = AuthManager.getInstance();
        initViews();
        setupAnimations();
        setupListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupAnimations() {
        CardView card = findViewById(R.id.cardRegister);
        card.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> performRegister());
        tvLogin.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }

    private void performRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty()) { etName.setError("Name required"); return; }
        if (email.isEmpty()) { etEmail.setError("Email required"); return; }
        if (phone.isEmpty()) { etPhone.setError("Phone required"); return; }
        if (password.length() < 6) { etPassword.setError("Min 6 characters"); return; }

        showLoading(true);
        authManager.register(email, password)
                .addOnSuccessListener(authResult -> {
                    authManager.updateDisplayName(name);
                    showLoading(false);
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!show);
    }
}
