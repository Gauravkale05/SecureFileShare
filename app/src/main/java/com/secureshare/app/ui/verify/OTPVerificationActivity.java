package com.secureshare.app.ui.verify;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.secureshare.app.databinding.ActivityOtpVerificationBinding;
import com.secureshare.app.firebase.StorageManager;
import com.secureshare.app.utils.Constants;
import com.secureshare.app.utils.OTPHelper;
import com.secureshare.app.utils.OtpSender;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class OTPVerificationActivity extends AppCompatActivity {

    private ActivityOtpVerificationBinding binding;
    private String fileId;
    private String email;
    private DocumentReference otpDocumentRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fileId = getIntent().getStringExtra(Constants.EXTRA_FILE_ID);
        email = getIntent().getStringExtra(Constants.EXTRA_ENTERED_EMAIL);
        if (fileId == null || email == null) {
            Toast.makeText(this, "Invalid verification request", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        otpDocumentRef = FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_OTP_VERIFICATION)
                .document(OTPHelper.buildOtpDocumentId(fileId, email));

        binding.tvOtpSubtitle.setText("Enter OTP sent to " + email);
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnVerify.setOnClickListener(v -> verifyOtp());
        binding.btnResendOtp.setOnClickListener(v -> resendOtp());
    }

    private void verifyOtp() {
        String enteredOtp = binding.etOtp.getText().toString().trim();
        if (enteredOtp.length() != 6) {
            binding.etOtp.setError("Enter valid 6-digit OTP");
            return;
        }

        setLoading(true);
        otpDocumentRef.get()
                .addOnSuccessListener(this::handleOtpVerification)
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void handleOtpVerification(DocumentSnapshot snapshot) {
        if (!snapshot.exists()) {
            setLoading(false);
            Toast.makeText(this, "OTP record not found. Please resend OTP.", Toast.LENGTH_LONG).show();
            return;
        }

        String storedOtp = snapshot.getString("otp");
        Timestamp expiresAt = snapshot.getTimestamp("expiresAt");
        Long attempts = snapshot.getLong("attempts");
        long attemptsCount = attempts == null ? 0 : attempts;

        if (attemptsCount >= 3) {
            setLoading(false);
            Toast.makeText(this, "Maximum attempts exceeded. Please resend OTP.", Toast.LENGTH_LONG).show();
            return;
        }

        if (expiresAt == null || expiresAt.toDate().before(new Date())) {
            setLoading(false);
            Toast.makeText(this, "OTP expired. Please resend OTP.", Toast.LENGTH_LONG).show();
            return;
        }

        String enteredOtp = binding.etOtp.getText().toString().trim();
        if (enteredOtp.equals(storedOtp)) {
            StorageManager.getInstance().updateStatus(fileId, Constants.STATUS_VERIFIED);
            setLoading(false);
            Toast.makeText(this, "OTP verified successfully", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
            return;
        }

        long updatedAttempts = attemptsCount + 1;
        otpDocumentRef.update("attempts", updatedAttempts)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    int remaining = (int) Math.max(0, 3 - updatedAttempts);
                    Toast.makeText(this, "OTP mismatch. Attempts left: " + remaining, Toast.LENGTH_LONG).show();
                });
    }

    private void resendOtp() {
        setLoading(true);
        String newOtp = OTPHelper.generateSixDigitOtp();
        Timestamp now = Timestamp.now();
        Timestamp expiresAt = new Timestamp(new Date(System.currentTimeMillis() + (5 * 60 * 1000)));

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("email", email);
        updateData.put("fileId", fileId);
        updateData.put("otp", newOtp);
        updateData.put("createdAt", now);
        updateData.put("expiresAt", expiresAt);
        updateData.put("attempts", 0);

        otpDocumentRef.set(updateData)
                .addOnSuccessListener(unused ->
                        OtpSender.sendViaEmail(email, newOtp, new OtpSender.OtpCallback() {
                            @Override
                            public void onSuccess() {
                                setLoading(false);
                                Toast.makeText(OTPVerificationActivity.this, "OTP resent successfully", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String message) {
                                setLoading(false);
                                Toast.makeText(OTPVerificationActivity.this, "Send failed: " + message, Toast.LENGTH_LONG).show();
                            }
                        })
                )
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnVerify.setEnabled(!loading);
        binding.btnResendOtp.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
