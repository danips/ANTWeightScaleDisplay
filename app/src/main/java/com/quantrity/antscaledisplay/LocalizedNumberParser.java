package com.quantrity.antscaledisplay;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

/** Strictly parses a complete localized numeric value without depending on Android views. */
final class LocalizedNumberParser {
    static final class Result {
        private static final Result INVALID = new Result(false, 0);

        private final boolean valid;
        private final double value;

        private Result(boolean valid, double value) {
            this.valid = valid;
            this.value = value;
        }

        static Result valid(double value) {
            return new Result(true, value);
        }

        static Result invalid() {
            return INVALID;
        }

        boolean isValid() {
            return valid;
        }

        double value() {
            if (!valid) throw new IllegalStateException("Numeric value is invalid");
            return value;
        }
    }

    private LocalizedNumberParser() {}

    static Result parse(String text) {
        return parse(text, Locale.getDefault());
    }

    static Result parse(String text, Locale locale) {
        if (text == null) return Result.invalid();
        String value = text.trim();
        if (value.isEmpty()) return Result.invalid();

        ParsePosition position = new ParsePosition(0);
        Number parsed = NumberFormat.getNumberInstance(locale).parse(value, position);
        if (parsed == null || position.getIndex() != value.length()) return Result.invalid();
        return Result.valid(parsed.doubleValue());
    }

    static double parseOrDefault(CharSequence text, double defaultValue) {
        Result parsed = parse(text == null ? null : text.toString());
        return parsed.isValid() ? parsed.value() : defaultValue;
    }
}
