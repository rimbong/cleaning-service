package com.boot.cleanhub.util.security;

public class StrIDCodec {

    public static String encodeStrID(String data) {
        if (data == null || data.isEmpty()) {
            return "";
        }

        int[] pattern = { 3, 6, 6, 3, 2, 3, 5, 7, 7, 5, 6, 4, 2, 3, 5, 0, 4, 7, 7, 5, 4, 7, 4, 5, 9, 7, 3, 2, 9, 3, 2, 9, 2 };
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < data.length(); i++) {
            int datanum = data.charAt(i);
            if (i < pattern.length) {
                datanum += pattern[i];
            }
            String num = String.format("%05d", datanum);
            result.append(num);
        }
        return result.toString();
    }

    public static String decodeStrID(String data) {   // ← decoder → decode 로도 자연스럽게 변경 추천
        if (data == null || data.isEmpty()) {
            return "";
        }

        int[] pattern = { 3, 6, 6, 3, 2, 3, 5, 7, 7, 5, 6, 4, 2, 3, 5, 0, 4, 7, 7, 5, 4, 7, 4, 5, 9, 7, 3, 2, 9, 3, 2, 9, 2 };
        StringBuilder input2Data = new StringBuilder();

        for (int i = 0; i + 4 < data.length(); i += 5) {
            String num = data.substring(i, i + 5);
            int value = Integer.parseInt(num);
            input2Data.append((char) value);
        }

        StringBuilder retVal = new StringBuilder();
        int len = input2Data.length();

        for (int i = 0; i < len; i++) {
            char ch = input2Data.charAt(i);
            if (i < pattern.length) {
                retVal.append((char) (ch - pattern[i]));
            } else {
                retVal.append(ch);
            }
        }
        return retVal.toString();
    }
}