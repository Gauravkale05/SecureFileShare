package com.secureshare.app.models;

public class SharedFile {
    private String fileId;
    private String fileName;
    private String senderEmail;
    private String senderUid;
    private String recipientEmail;
    private String recipientPhone;
    private String aesKey;
    private String desKey;
    private String status;
    private long timestamp;
    private long fileSize;
    private String verificationCode;

    // Required empty constructor for Firestore
    public SharedFile() {}

    // Getters
    public String getFileId() { return fileId; }
    public String getFileName() { return fileName; }
    public String getSenderEmail() { return senderEmail; }
    public String getSenderUid() { return senderUid; }
    public String getRecipientEmail() { return recipientEmail; }
    public String getRecipientPhone() { return recipientPhone; }
    public String getAesKey() { return aesKey; }
    public String getDesKey() { return desKey; }
    public String getStatus() { return status; }
    public long getTimestamp() { return timestamp; }
    public long getFileSize() { return fileSize; }
    public String getVerificationCode() { return verificationCode; }

    // Setters
    public void setFileId(String fileId) { this.fileId = fileId; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }
    public void setSenderUid(String senderUid) { this.senderUid = senderUid; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }
    public void setAesKey(String aesKey) { this.aesKey = aesKey; }
    public void setDesKey(String desKey) { this.desKey = desKey; }
    public void setStatus(String status) { this.status = status; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
}
