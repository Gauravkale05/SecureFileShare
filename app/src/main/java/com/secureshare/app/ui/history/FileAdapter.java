package com.secureshare.app.ui.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.secureshare.app.R;
import com.secureshare.app.models.SharedFile;
import com.secureshare.app.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(SharedFile file);
    }

    private final List<SharedFile> files;
    private final OnItemClickListener listener;
    private final boolean showAction;

    public FileAdapter(List<SharedFile> files, OnItemClickListener listener, boolean showAction) {
        this.files = files;
        this.listener = listener;
        this.showAction = showAction;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(files.get(position));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName, tvFileInfo, tvStatus;
        Button btnAction;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileInfo = itemView.findViewById(R.id.tvFileInfo);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnAction = itemView.findViewById(R.id.btnAction);
        }

        void bind(SharedFile file) {
            tvFileName.setText(file.getFileName());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            String info = "From: " + file.getSenderEmail() + " \u2022 " +
                    sdf.format(new Date(file.getTimestamp()));
            tvFileInfo.setText(info);

            // Status with color
            switch (file.getStatus()) {
                case Constants.STATUS_PENDING:
                    tvStatus.setText("\u23F3 Pending");
                    tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_pending));
                    break;
                case Constants.STATUS_VERIFIED:
                    tvStatus.setText("\u2713 Verified");
                    tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_verified));
                    break;
                case Constants.STATUS_DOWNLOADED:
                    tvStatus.setText("\u2713 Downloaded");
                    tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_downloaded));
                    break;
            }

            // Action button (only in download mode)
            if (showAction && !file.getStatus().equals(Constants.STATUS_DOWNLOADED)) {
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setText(file.getStatus().equals(Constants.STATUS_PENDING) ? "Verify" : "Download");
                btnAction.setOnClickListener(v -> {
                    if (listener != null) listener.onItemClick(file);
                });
            } else {
                btnAction.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(file);
            });
        }
    }
}
