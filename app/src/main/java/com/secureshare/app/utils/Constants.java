package com.secureshare.app.utils;

public class Constants {
    public static final String COLLECTION_SHARED_FILES = "shared_files";
    public static final String COLLECTION_OTP_VERIFICATION = "otp_verification";
    public static final String STORAGE_FILES_PATH = "files";

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_VERIFIED = "verified";
    public static final String STATUS_DOWNLOADED = "downloaded";

    public static final String EXTRA_FILE_ID = "extra_file_id";
    public static final String EXTRA_PHONE_NUMBER = "extra_phone_number";
    public static final String EXTRA_RECIPIENT_EMAIL = "extra_recipient_email";
    public static final String EXTRA_ENTERED_EMAIL = "extra_entered_email";

    // ─── Gmail SMTP (OTP via Email – FREE) ───────────────────────────────────
    // Setup:
    //  1. Enable 2-Step Verification on your Gmail account.
    //  2. Generate an App Password at: https://myaccount.google.com/apppasswords
    //  3. Replace the two values below with your sender Gmail and the 16-char App Password.
    public static final String GMAIL_SENDER_EMAIL = "codeash876@gmail.com";
    public static final String GMAIL_SENDER_APP_PASSWORD = "hudr pjja rqcy tvdk"; // 16-char App Password

    // ─── Textbelt SMS (OTP via SMS) ──────────────────────────────────────────
    // Free tier: key "textbelt" sends 1 free SMS per day (sufficient for dev/demo).
    // For production, buy a key at https://textbelt.com and replace the value below.
    public static final String TEXTBELT_API_KEY = "textbelt";
}
