package com.secureshare.app.utils;

import java.util.Locale;
import java.util.Random;

public final class OTPHelper {

    private OTPHelper() {
    }

    public static String generateSixDigitOtp() {
        int otp = 100000 + new Random().nextInt(900000);
        return String.valueOf(otp);
    }

    public static String buildOtpDocumentId(String fileId, String email) {
        String safeEmail = email == null ? "" : email.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "_");
        return fileId + "_" + safeEmail;
    }
}
