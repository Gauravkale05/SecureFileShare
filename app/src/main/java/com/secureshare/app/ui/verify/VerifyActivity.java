package com.secureshare.app.ui.verify;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.secureshare.app.R;
import com.secureshare.app.firebase.StorageManager;
import com.secureshare.app.utils.Constants;
import com.secureshare.app.utils.OtpSender;

public class VerifyActivity extends AppCompatActivity {

    private TextView tvRecipientInfo;
    private RadioGroup rgDelivery;
    private EditText etCode;
    private Button btnSendCode, btnVerify;
    private ProgressBar progressBar;

    private String fileId, phoneNumber, recipientEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);

        fileId         = getIntent().getStringExtra(Constants.EXTRA_FILE_ID);
        phoneNumber    = getIntent().getStringExtra(Constants.EXTRA_PHONE_NUMBER);
        recipientEmail = getIntent().getStringExtra(Constants.EXTRA_RECIPIENT_EMAIL);

        initViews();
        setupListeners();
    }

    private void initViews() {
        tvRecipientInfo = findViewById(R.id.tvRecipientInfo);
        rgDelivery      = findViewById(R.id.rgDelivery);
        etCode          = findViewById(R.id.etCode);
        btnSendCode     = findViewById(R.id.btnSendCode);
        btnVerify       = findViewById(R.id.btnVerify);
        progressBar     = findViewById(R.id.progressBar);

        // Default selection: Email
        updateRecipientLabel(true);
        btnVerify.setEnabled(false);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        rgDelivery.setOnCheckedChangeListener((group, checkedId) ->
                updateRecipientLabel(checkedId == R.id.rbEmail));

        btnSendCode.setOnClickListener(v -> sendVerificationCode());
        btnVerify.setOnClickListener(v -> verifyCode());
    }

    private void updateRecipientLabel(boolean useEmail) {
        String dest = useEmail ? recipientEmail : phoneNumber;
        tvRecipientInfo.setText("OTP will be sent to:\n" + dest);
    }

    private void sendVerificationCode() {
        progressBar.setVisibility(View.VISIBLE);
        btnSendCode.setEnabled(false);

        // Generate random 6-digit OTP
        String code = String.valueOf((int) (Math.random() * 900000) + 100000);

        // Persist OTP in Firestore, then dispatch via chosen channel
        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_SHARED_FILES)
                .document(fileId)
                .update("verificationCode", code)
                .addOnSuccessListener(aVoid -> dispatchOtp(code))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSendCode.setEnabled(true);
                    Toast.makeText(this, "Failed to generate OTP: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /** Sends the OTP via Email or SMS depending on the user's radio selection. */
    private void dispatchOtp(String code) {
        boolean useEmail = rgDelivery.getCheckedRadioButtonId() == R.id.rbEmail;

        OtpSender.OtpCallback cb = new OtpSender.OtpCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                btnVerify.setEnabled(true);
                String dest = useEmail ? recipientEmail : phoneNumber;
                Toast.makeText(VerifyActivity.this,
                        "OTP sent to " + dest, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                btnSendCode.setEnabled(true);
                Toast.makeText(VerifyActivity.this,
                        "Delivery failed: " + message, Toast.LENGTH_LONG).show();
            }
        };

        if (useEmail) {
            OtpSender.sendViaEmail(recipientEmail, code, cb);
        } else {
            OtpSender.sendViaSms(phoneNumber, code, cb);
        }
    }

    private void verifyCode() {
        String enteredCode = etCode.getText().toString().trim();
        if (enteredCode.isEmpty() || enteredCode.length() < 6) {
            etCode.setError("Enter valid 6-digit code");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_SHARED_FILES)
                .document(fileId)
                .get()
                .addOnSuccessListener(doc -> {
                    progressBar.setVisibility(View.GONE);
                    String storedCode = doc.getString("verificationCode");

                    if (enteredCode.equals(storedCode)) {
                        StorageManager.getInstance().updateStatus(fileId, Constants.STATUS_VERIFIED);
                        Toast.makeText(this, "Verification successful!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(this, "Invalid code. Try again.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
