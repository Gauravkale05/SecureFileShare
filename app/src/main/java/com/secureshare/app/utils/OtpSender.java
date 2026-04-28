package com.secureshare.app.utils;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Delivers OTP codes via two free channels:
 *
 * 1. EMAIL — Gmail SMTP (truly free, unlimited).
 *    Before use, fill in Constants.GMAIL_SENDER_EMAIL and Constants.GMAIL_SENDER_APP_PASSWORD.
 *    How to get an App Password:
 *      a) Enable 2-Step Verification on the sender Gmail account.
 *      b) Visit https://myaccount.google.com/apppasswords
 *      c) Create a password for "Mail / Android device" and copy the 16-char key.
 *
 * 2. SMS — Textbelt API (https://textbelt.com).
 *    The built-in key "textbelt" provides 1 free SMS per day — enough for development.
 *    For production, replace Constants.TEXTBELT_API_KEY with a purchased key.
 */
public class OtpSender {

    public interface OtpCallback {
        void onSuccess();
        void onError(String message);
    }

    // ─── EMAIL via Gmail SMTP ─────────────────────────────────────────────────

    public static void sendViaEmail(String toEmail, String otp, OtpCallback callback) {
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");
                props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                Constants.GMAIL_SENDER_EMAIL,
                                Constants.GMAIL_SENDER_APP_PASSWORD
                        );
                    }
                });

                Message msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(Constants.GMAIL_SENDER_EMAIL, "SecureShare"));
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                msg.setSubject("SecureShare \u2013 Your Verification Code");
                msg.setText(
                        "Hello,\n\n"
                        + "Your SecureShare one-time verification code is:\n\n"
                        + "        " + otp + "\n\n"
                        + "This code is valid for 10 minutes.\n"
                        + "If you did not request this, please ignore this message.\n\n"
                        + "\u2014 SecureShare Team"
                );
                Transport.send(msg);
                postMain(callback::onSuccess);

            } catch (Exception e) {
                postMain(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    // ─── SMS via Textbelt API ─────────────────────────────────────────────────

    public static void sendViaSms(String phoneNumber, String otp, OtpCallback callback) {
        new Thread(() -> {
            try {
                String body = "phone=" + URLEncoder.encode(phoneNumber, "UTF-8")
                        + "&message=" + URLEncoder.encode(
                                "SecureShare OTP: " + otp + ". Valid for 10 minutes.", "UTF-8")
                        + "&key=" + URLEncoder.encode(Constants.TEXTBELT_API_KEY, "UTF-8");

                URL url = new URL("https://textbelt.com/text");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15_000);
                conn.setReadTimeout(15_000);
                conn.setRequestProperty("Content-Type",
                        "application/x-www-form-urlencoded");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes("UTF-8"));
                }

                // Read JSON response
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                }

                JSONObject json = new JSONObject(sb.toString());
                if (json.optBoolean("success", false)) {
                    postMain(callback::onSuccess);
                } else {
                    String error = json.optString("error", "SMS delivery failed");
                    postMain(() -> callback.onError(error));
                }

            } catch (Exception e) {
                postMain(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private static void postMain(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }
}
