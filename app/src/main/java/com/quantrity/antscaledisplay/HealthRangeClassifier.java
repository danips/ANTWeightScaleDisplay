package com.quantrity.antscaledisplay;

/** Classifies body-composition values into the health ranges rendered by the UI. */
final class HealthRangeClassifier {
    private HealthRangeClassifier() {}

    private static byte range(float[] limits, float value) {
        if (value < limits[0]) return 0;
        if (value < limits[1]) return 1;
        if (value <= limits[2]) return 2;
        return 3;
    }

    static byte getPercentHydrationDesc(float hydration, boolean male) {
        float[] limits = male ? new float[]{50, 65} : new float[]{45, 60};
        if (hydration < limits[0]) return 0;
        return hydration <= limits[1] ? (byte) 1 : (byte) 2;
    }

    static byte getBoneMassDesc(float weight, float boneMass, boolean male) {
        float expected;
        if (male) expected = weight < 65 ? 2.66f : weight < 95 ? 3.29f : 3.69f;
        else expected = weight < 50 ? 1.95f : weight < 75 ? 2.40f : 2.95f;
        return boneMass < 0.9 * expected ? (byte) 0 : (byte) 1;
    }

    static byte getBMIDesc(byte age, float bmi, boolean male) {
        if (age >= 19 && age <= 24) return range(new float[]{19, 24, 30}, bmi);
        if (age <= 34 && age >= 25) return range(new float[]{20, 25, 30.5f}, bmi);
        if (age <= 44 && age >= 35) return range(new float[]{21, 26, 31}, bmi);
        if (age <= 54 && age >= 45) return range(new float[]{22, 27, 31.5f}, bmi);
        if (age <= 65 && age >= 55) return range(new float[]{23, 28, 32}, bmi);
        if (age > 65) return range(new float[]{24, 29, 32.5f}, bmi);
        float[] limits = bmiChildLimits(age, male);
        return limits == null ? -1 : range(limits, bmi);
    }

    private static float[] bmiChildLimits(byte age, boolean male) {
        if (male) {
            if (age == 7 || age == 8) return new float[]{14, 18.6f, 20.6f};
            if (age == 9 || age == 10) return new float[]{14.4f, 20.2f, 22.8f};
            if (age == 11 || age == 12) return new float[]{15.2f, 21.8f, 25};
            if (age == 13 || age == 14) return new float[]{16.2f, 23.4f, 26.26f};
            if (age == 15 || age == 16) return new float[]{17.3f, 24, 27.8f};
            if (age == 17 || age == 18) return new float[]{18.4f, 24, 28.6f};
        } else {
            if (age == 7 || age == 8) return new float[]{13.8f, 18.9f, 20.9f};
            if (age == 9 || age == 10) return new float[]{14.3f, 20.4f, 23};
            if (age == 11 || age == 12) return new float[]{15.1f, 22, 25};
            if (age == 13 || age == 14) return new float[]{16.4f, 23.7f, 26.7f};
            if (age == 15 || age == 16) return new float[]{17.5f, 24.8f, 27.6f};
            if (age == 17 || age == 18) return new float[]{18.2f, 24, 27.8f};
        }
        return null;
    }

    static byte getPercentFatDesc(byte age, float fat, boolean male) {
        if (male) {
            if (age >= 20 && age <= 39) return range(new float[]{8, 20, 25}, fat);
            if (age >= 40 && age <= 59) return range(new float[]{11, 22, 28}, fat);
            if (age >= 60 && age <= 79) return range(new float[]{13, 25, 30}, fat);
        } else {
            if (age >= 20 && age <= 39) return range(new float[]{21, 33, 39}, fat);
            if (age >= 40 && age <= 59) return range(new float[]{23, 34, 40}, fat);
            if (age >= 60 && age <= 79) return range(new float[]{24, 36, 42}, fat);
        }
        float[] limits = fatChildLimits(age, male);
        return limits == null ? -1 : range(limits, fat);
    }

    private static float[] fatChildLimits(byte age, boolean male) {
        if (male) {
            switch (age) {
                case 7: return new float[]{13, 20, 25};
                case 8: return new float[]{13, 21, 26};
                case 9: return new float[]{13, 22, 27};
                case 10:
                case 11:
                case 12: return new float[]{13, 23, 38};
                case 13: return new float[]{12, 22, 27};
                case 14: return new float[]{12, 21, 26};
                case 15: return new float[]{11, 21, 24};
                case 16:
                case 17:
                case 18: return new float[]{10, 20, 24};
                case 19: return new float[]{9, 20, 24};
                default: return null;
            }
        }
        switch (age) {
            case 7: return new float[]{15, 25, 29};
            case 8: return new float[]{15, 26, 30};
            case 9: return new float[]{16, 27, 31};
            case 10: return new float[]{16, 28, 32};
            case 11:
            case 12:
            case 13: return new float[]{16, 29, 33};
            case 14:
            case 15:
            case 16: return new float[]{16, 30, 34};
            case 17: return new float[]{16, 30, 35};
            case 18: return new float[]{17, 31, 36};
            case 19: return new float[]{19, 32, 37};
            default: return null;
        }
    }
}
