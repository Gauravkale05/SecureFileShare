package com.secureshare.app.encryption;

import java.util.Arrays;

/**
 * Splits a file's bytes into two halves and merges them back.
 * First half -> AES encryption, Second half -> DES encryption
 */
public class FileSplitter {

    public static byte[][] split(byte[] data) {
        int mid = data.length / 2;
        byte[] firstHalf = Arrays.copyOfRange(data, 0, mid);
        byte[] secondHalf = Arrays.copyOfRange(data, mid, data.length);
        return new byte[][] { firstHalf, secondHalf };
    }

    public static byte[] merge(byte[] firstHalf, byte[] secondHalf) {
        byte[] result = new byte[firstHalf.length + secondHalf.length];
        System.arraycopy(firstHalf, 0, result, 0, firstHalf.length);
        System.arraycopy(secondHalf, 0, result, firstHalf.length, secondHalf.length);
        return result;
    }
}
