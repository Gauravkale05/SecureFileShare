package com.secureshare.app.ui.upload;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.secureshare.app.R;
import com.secureshare.app.encryption.AESHelper;
import com.secureshare.app.encryption.DESHelper;
import com.secureshare.app.encryption.FileSplitter;
import com.secureshare.app.firebase.AuthManager;
import com.secureshare.app.firebase.StorageManager;
import com.secureshare.app.models.SharedFile;
import com.secureshare.app.utils.Constants;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class UploadActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 1;

    private MaterialCardView cardSelectFile;
    private TextView tvSelectedFile, tvProgress;
    private EditText etRecipientEmail, etRecipientPhone;
    private Button btnUpload;
    private ProgressBar progressBar;

    private Uri selectedFileUri;
    private String selectedFileName;
    private long selectedFileSize;

    private AuthManager authManager;
    private StorageManager storageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        authManager = AuthManager.getInstance();
        storageManager = StorageManager.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        cardSelectFile = findViewById(R.id.cardSelectFile);
        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        etRecipientEmail = findViewById(R.id.etRecipientEmail);
        etRecipientPhone = findViewById(R.id.etRecipientPhone);
        btnUpload = findViewById(R.id.btnUpload);
        progressBar = findViewById(R.id.progressBar);
        tvProgress = findViewById(R.id.tvProgress);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        cardSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(Intent.createChooser(intent, "Select File"), PICK_FILE_REQUEST);
        });
        btnUpload.setOnClickListener(v -> performUpload());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            if (selectedFileUri != null) {
                selectedFileName = getFileName(selectedFileUri);
                selectedFileSize = getFileSize(selectedFileUri);
                tvSelectedFile.setText(selectedFileName + " (" + formatSize(selectedFileSize) + ")");
                tvSelectedFile.setTextColor(getResources().getColor(R.color.text_primary));
            }
        }
    }

    private void performUpload() {
        if (selectedFileUri == null) {
            Toast.makeText(this, "Please select a file", Toast.LENGTH_SHORT).show();
            return;
        }
        String recipientEmail = etRecipientEmail.getText().toString().trim().toLowerCase();
        String recipientPhone = etRecipientPhone.getText().toString().trim();
        if (recipientEmail.isEmpty()) { etRecipientEmail.setError("Required"); return; }
        if (recipientPhone.isEmpty()) { etRecipientPhone.setError("Required"); return; }

        showLoading(true, "Reading file...");

        new Thread(() -> {
            try {
                // Step 1: Read file
                byte[] fileBytes = readFileBytes(selectedFileUri);
                updateProgress("Splitting file...");

                // Step 2: Split into two halves
                byte[][] parts = FileSplitter.split(fileBytes);
                updateProgress("Encrypting with AES...");

                // Step 3: Encrypt each half
                String aesKey = AESHelper.generateKey();
                byte[] aesPart = AESHelper.encrypt(parts[0], aesKey);
                updateProgress("Encrypting with DES...");

                String desKey = DESHelper.generateKey();
                byte[] desPart = DESHelper.encrypt(parts[1], desKey);
                updateProgress("Uploading encrypted parts...");

                // Step 4: Upload both parts to Firebase Storage
                String fileId = storageManager.generateFileId();

                storageManager.uploadAESPart(fileId, aesPart)
                    .addOnSuccessListener(t1 -> {
                        storageManager.uploadDESPart(fileId, desPart)
                            .addOnSuccessListener(t2 -> {
                                updateProgress("Saving metadata...");

                                // Step 5: Save metadata to Firestore
                                SharedFile sf = new SharedFile();
                                sf.setFileId(fileId);
                                sf.setFileName(selectedFileName);
                                sf.setSenderEmail(authManager.getCurrentUser().getEmail().toLowerCase());
                                sf.setSenderUid(authManager.getCurrentUser().getUid());
                                sf.setRecipientEmail(recipientEmail.toLowerCase());
                                sf.setRecipientPhone(recipientPhone);
                                sf.setAesKey(aesKey);
                                sf.setDesKey(desKey);
                                sf.setStatus(Constants.STATUS_PENDING);
                                sf.setTimestamp(System.currentTimeMillis());
                                sf.setFileSize(selectedFileSize);

                                storageManager.saveFileMetadata(sf)
                                    .addOnSuccessListener(v -> {
                                        showLoading(false, "");
                                        Toast.makeText(this, "File encrypted & uploaded!", Toast.LENGTH_LONG).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> showError("Metadata save failed: " + e.getMessage()));
                            })
                            .addOnFailureListener(e -> showError("DES upload failed: " + e.getMessage()));
                    })
                    .addOnFailureListener(e -> showError("AES upload failed: " + e.getMessage()));

            } catch (Exception e) {
                showError("Error: " + e.getMessage());
            }
        }).start();
    }

    private byte[] readFileBytes(Uri uri) throws Exception {
        InputStream is = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int n;
        while ((n = is.read(data)) != -1) buffer.write(data, 0, n);
        is.close();
        return buffer.toByteArray();
    }

    private String getFileName(Uri uri) {
        String name = "unknown";
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();
            name = cursor.getString(idx);
            cursor.close();
        }
        return name;
    }

    private long getFileSize(Uri uri) {
        long size = 0;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int idx = cursor.getColumnIndex(OpenableColumns.SIZE);
            cursor.moveToFirst();
            size = cursor.getLong(idx);
            cursor.close();
        }
        return size;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void updateProgress(String msg) {
        runOnUiThread(() -> tvProgress.setText(msg));
    }

    private void showError(String msg) {
        runOnUiThread(() -> {
            showLoading(false, "");
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        });
    }

    private void showLoading(boolean show, String msg) {
        runOnUiThread(() -> {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            tvProgress.setVisibility(show ? View.VISIBLE : View.GONE);
            tvProgress.setText(msg);
            btnUpload.setEnabled(!show);
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
