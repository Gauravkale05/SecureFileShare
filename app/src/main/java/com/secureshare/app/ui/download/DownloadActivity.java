package com.secureshare.app.ui.download;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.secureshare.app.R;
import com.secureshare.app.encryption.AESHelper;
import com.secureshare.app.encryption.DESHelper;
import com.secureshare.app.encryption.FileSplitter;
import com.secureshare.app.firebase.AuthManager;
import com.secureshare.app.firebase.StorageManager;
import com.secureshare.app.models.SharedFile;
import com.secureshare.app.ui.history.FileAdapter;
import com.secureshare.app.ui.verify.EnterEmailActivity;
import com.secureshare.app.ui.verify.OTPVerificationActivity;
import com.secureshare.app.utils.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class DownloadActivity extends AppCompatActivity implements FileAdapter.OnItemClickListener {

    private static final int ENTER_EMAIL_REQUEST = 100;
    private static final int VERIFY_REQUEST = 101;

    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvProgress;
    private final List<SharedFile> fileList = new ArrayList<>();
    private SharedFile pendingDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        initViews();
        loadFiles();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvProgress = findViewById(R.id.tvProgress);

        adapter = new FileAdapter(fileList, this, true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void loadFiles() {
        progressBar.setVisibility(View.VISIBLE);
        String email = AuthManager.getInstance().getCurrentUser().getEmail().toLowerCase();

        StorageManager.getInstance().getReceivedFiles(email)
                .get()
                .addOnSuccessListener(query -> {
                    progressBar.setVisibility(View.GONE);
                    fileList.clear();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        SharedFile f = doc.toObject(SharedFile.class);
                        if (f != null) fileList.add(f);
                    }
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(fileList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load files", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onItemClick(SharedFile file) {
        if (Constants.STATUS_PENDING.equals(file.getStatus())) {
            pendingDownload = file;
            Intent enterEmailIntent = new Intent(this, EnterEmailActivity.class);
            enterEmailIntent.putExtra(Constants.EXTRA_FILE_ID, file.getFileId());
            enterEmailIntent.putExtra(Constants.EXTRA_RECIPIENT_EMAIL, file.getRecipientEmail());
            startActivityForResult(enterEmailIntent, ENTER_EMAIL_REQUEST);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else if (Constants.STATUS_VERIFIED.equals(file.getStatus())) {
            downloadAndDecrypt(file);
        } else {
            Toast.makeText(this, "File already downloaded", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || pendingDownload == null || data == null) {
            return;
        }

        if (requestCode == ENTER_EMAIL_REQUEST) {
            String verifiedEmail = data.getStringExtra(Constants.EXTRA_ENTERED_EMAIL);
            if (verifiedEmail == null || verifiedEmail.trim().isEmpty()) {
                Toast.makeText(this, "Invalid email verification data", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent verifyIntent = new Intent(this, OTPVerificationActivity.class);
            verifyIntent.putExtra(Constants.EXTRA_FILE_ID, pendingDownload.getFileId());
            verifyIntent.putExtra(Constants.EXTRA_ENTERED_EMAIL, verifiedEmail);
            startActivityForResult(verifyIntent, VERIFY_REQUEST);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            return;
        }

        if (requestCode == VERIFY_REQUEST) {
            pendingDownload.setStatus(Constants.STATUS_VERIFIED);
            downloadAndDecrypt(pendingDownload);
        }
    }

    private void downloadAndDecrypt(SharedFile file) {
        tvProgress.setVisibility(View.VISIBLE);
        tvProgress.setText("Downloading AES part...");
        progressBar.setVisibility(View.VISIBLE);

        StorageManager.getInstance().downloadAESPart(file.getFileId())
                .addOnSuccessListener(aesData -> {
                    tvProgress.setText("Downloading DES part...");
                    StorageManager.getInstance().downloadDESPart(file.getFileId())
                            .addOnSuccessListener(desData -> {
                                tvProgress.setText("Decrypting & merging...");
                                new Thread(() -> {
                                    try {
                                        byte[] part1 = AESHelper.decrypt(aesData, file.getAesKey());
                                        byte[] part2 = DESHelper.decrypt(desData, file.getDesKey());
                                        byte[] merged = FileSplitter.merge(part1, part2);

                                        File dir = Environment.getExternalStoragePublicDirectory(
                                                Environment.DIRECTORY_DOWNLOADS);
                                        File out = new File(dir, file.getFileName());
                                        FileOutputStream fos = new FileOutputStream(out);
                                        fos.write(merged);
                                        fos.close();

                                        StorageManager.getInstance().updateStatus(
                                                file.getFileId(), Constants.STATUS_DOWNLOADED);

                                        runOnUiThread(() -> {
                                            hideProgress();
                                            Toast.makeText(this,
                                                    "Saved to Downloads/" + file.getFileName(),
                                                    Toast.LENGTH_LONG).show();
                                            loadFiles();
                                        });
                                    } catch (Exception e) {
                                        runOnUiThread(() -> {
                                            hideProgress();
                                            Toast.makeText(this,
                                                    "Decryption failed: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        });
                                    }
                                }).start();
                            })
                            .addOnFailureListener(e -> {
                                hideProgress();
                                showError();
                            });
                })
                .addOnFailureListener(e -> {
                    hideProgress();
                    showError();
                });
    }

    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
        tvProgress.setVisibility(View.GONE);
    }

    private void showError() {
        Toast.makeText(this, "Download failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
