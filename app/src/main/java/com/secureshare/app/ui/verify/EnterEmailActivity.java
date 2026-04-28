package com.secureshare.app.ui.verify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.secureshare.app.databinding.ActivityEnterEmailBinding;
import com.secureshare.app.utils.Constants;
import com.secureshare.app.utils.OTPHelper;
import com.secureshare.app.utils.OtpSender;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class EnterEmailActivity extends AppCompatActivity {

    private ActivityEnterEmailBinding binding;
    private String fileId;
    private String expectedRecipientEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEnterEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fileId = getIntent().getStringExtra(Constants.EXTRA_FILE_ID);
        expectedRecipientEmail = getIntent().getStringExtra(Constants.EXTRA_RECIPIENT_EMAIL);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnContinue.setOnClickListener(v -> createAndSendOtp());
    }

    private void createAndSendOtp() {
        if (fileId == null || fileId.trim().isEmpty()) {
            Toast.makeText(this, "Invalid file reference", Toast.LENGTH_LONG).show();
            return;
        }
        String email = binding.etEmail.getText().toString().trim().toLowerCase();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Enter a valid email");
            return;
        }

        // Extra check: ensure OTP is requested for the intended recipient.
        if (expectedRecipientEmail != null && !expectedRecipientEmail.equalsIgnoreCase(email)) {
            Toast.makeText(this, "Entered email does not match recipient", Toast.LENGTH_LONG).show();
            return;
        }

        setLoading(true);
        String otp = OTPHelper.generateSixDigitOtp();
        Timestamp now = Timestamp.now();
        Timestamp expiresAt = new Timestamp(new Date(System.currentTimeMillis() + (5 * 60 * 1000)));

        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("fileId", fileId);
        data.put("otp", otp);
        data.put("createdAt", now);
        data.put("expiresAt", expiresAt);
        data.put("attempts", 0);

        String documentId = OTPHelper.buildOtpDocumentId(fileId, email);

        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_OTP_VERIFICATION)
                .document(documentId)
                .set(data)
                .addOnSuccessListener(unused -> sendOtpEmail(email, otp))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void sendOtpEmail(String email, String otp) {
        OtpSender.sendViaEmail(email, otp, new OtpSender.OtpCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(EnterEmailActivity.this, "OTP sent to " + email, Toast.LENGTH_LONG).show();
                Intent result = new Intent();
                result.putExtra(Constants.EXTRA_ENTERED_EMAIL, email);
                setResult(RESULT_OK, result);
                finish();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(EnterEmailActivity.this, "Failed to send OTP: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnContinue.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
