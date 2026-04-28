package com.secureshare.app.ui.history;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.secureshare.app.R;
import com.secureshare.app.firebase.AuthManager;
import com.secureshare.app.firebase.StorageManager;
import com.secureshare.app.models.SharedFile;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private Button btnSent, btnReceived;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private List<SharedFile> fileList = new ArrayList<>();
    private boolean showingSent = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        initViews();
        loadSentFiles();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        btnSent = findViewById(R.id.btnSent);
        btnReceived = findViewById(R.id.btnReceived);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        adapter = new FileAdapter(fileList, null, false);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnSent.setOnClickListener(v -> { showingSent = true; updateTabs(); loadSentFiles(); });
        btnReceived.setOnClickListener(v -> { showingSent = false; updateTabs(); loadReceivedFiles(); });
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        updateTabs();
    }

    private void updateTabs() {
        btnSent.setBackgroundResource(showingSent ? R.drawable.gradient_button : R.drawable.rounded_edittext);
        btnSent.setTextColor(getResources().getColor(showingSent ? R.color.white : R.color.text_secondary));
        btnReceived.setBackgroundResource(!showingSent ? R.drawable.gradient_button : R.drawable.rounded_edittext);
        btnReceived.setTextColor(getResources().getColor(!showingSent ? R.color.white : R.color.text_secondary));
    }

    private void loadSentFiles() {
        progressBar.setVisibility(View.VISIBLE);
        StorageManager.getInstance().getSentFiles(AuthManager.getInstance().getCurrentUser().getUid())
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
                    Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadReceivedFiles() {
        progressBar.setVisibility(View.VISIBLE);
        StorageManager.getInstance().getReceivedFiles(AuthManager.getInstance().getCurrentUser().getEmail().toLowerCase())
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
                    Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
