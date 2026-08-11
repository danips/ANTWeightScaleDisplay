package com.quantrity.antscaledisplay;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts an MFA code only from notifications that identify Garmin as the sender/service. */
final class MfaNotificationParser {
    private static final Pattern GARMIN_KEYWORD = Pattern.compile(
            "(?<![\\p{L}\\p{N}])garmin(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SIX_DIGIT_CODE = Pattern.compile("(?<!\\d)(\\d{6})(?!\\d)");

    private MfaNotificationParser() {}

    static String findCode(CharSequence title, CharSequence content) {
        String text = String.valueOf(title == null ? "" : title)
                + '\n' + String.valueOf(content == null ? "" : content);
        if (!GARMIN_KEYWORD.matcher(text).find()) return null;

        Matcher code = SIX_DIGIT_CODE.matcher(text);
        return code.find() ? code.group(1) : null;
    }
}
