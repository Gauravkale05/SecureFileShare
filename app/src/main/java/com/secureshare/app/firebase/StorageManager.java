package com.secureshare.app.firebase;

import android.util.Base64;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.secureshare.app.models.SharedFile;
import com.secureshare.app.utils.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageManager {
    private static StorageManager instance;
    private final FirebaseFirestore firestore;

    private static final String COLLECTION_FILE_PARTS = "file_parts";

    private StorageManager() {
        firestore = FirebaseFirestore.getInstance();
    }

    public static synchronized StorageManager getInstance() {
        if (instance == null) {
            instance = new StorageManager();
        }
        return instance;
    }

    public String generateFileId() {
        return UUID.randomUUID().toString();
    }

    // Upload encrypted AES part as Base64 to Firestore
    public Task<Void> uploadAESPart(String fileId, byte[] data) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("data", Base64.encodeToString(data, Base64.DEFAULT));
        doc.put("fileId", fileId);
        return firestore.collection(COLLECTION_FILE_PARTS)
                .document(fileId + "_aes")
                .set(doc);
    }

    // Upload encrypted DES part as Base64 to Firestore
    public Task<Void> uploadDESPart(String fileId, byte[] data) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("data", Base64.encodeToString(data, Base64.DEFAULT));
        doc.put("fileId", fileId);
        return firestore.collection(COLLECTION_FILE_PARTS)
                .document(fileId + "_des")
                .set(doc);
    }

    // Download AES part from Firestore and decode Base64
    public Task<byte[]> downloadAESPart(String fileId) {
        return firestore.collection(COLLECTION_FILE_PARTS)
                .document(fileId + "_aes")
                .get()
                .continueWith(task -> {
                    DocumentSnapshot doc = task.getResult();
                    String base64 = doc.getString("data");
                    return Base64.decode(base64, Base64.DEFAULT);
                });
    }

    // Download DES part from Firestore and decode Base64
    public Task<byte[]> downloadDESPart(String fileId) {
        return firestore.collection(COLLECTION_FILE_PARTS)
                .document(fileId + "_des")
                .get()
                .continueWith(task -> {
                    DocumentSnapshot doc = task.getResult();
                    String base64 = doc.getString("data");
                    return Base64.decode(base64, Base64.DEFAULT);
                });
    }

    // Save file share metadata to Firestore
    public Task<Void> saveFileMetadata(SharedFile sharedFile) {
        Map<String, Object> data = new HashMap<>();
        data.put("fileId", sharedFile.getFileId());
        data.put("fileName", sharedFile.getFileName());
        data.put("senderEmail", sharedFile.getSenderEmail());
        data.put("senderUid", sharedFile.getSenderUid());
        data.put("recipientEmail", sharedFile.getRecipientEmail());
        data.put("recipientPhone", sharedFile.getRecipientPhone());
        data.put("aesKey", sharedFile.getAesKey());
        data.put("desKey", sharedFile.getDesKey());
        data.put("status", sharedFile.getStatus());
        data.put("timestamp", sharedFile.getTimestamp());
        data.put("fileSize", sharedFile.getFileSize());

        return firestore.collection(Constants.COLLECTION_SHARED_FILES)
                .document(sharedFile.getFileId())
                .set(data);
    }

    // Update file status (pending -> verified -> downloaded)
    public Task<Void> updateStatus(String fileId, String status) {
        return firestore.collection(Constants.COLLECTION_SHARED_FILES)
                .document(fileId)
                .update("status", status);
    }

    // Query files sent by current user
    public Query getSentFiles(String uid) {
        return firestore.collection(Constants.COLLECTION_SHARED_FILES)
                .whereEqualTo("senderUid", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    // Query files received by current user
    public Query getReceivedFiles(String email) {
        return firestore.collection(Constants.COLLECTION_SHARED_FILES)
                .whereEqualTo("recipientEmail", email)
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }
}
