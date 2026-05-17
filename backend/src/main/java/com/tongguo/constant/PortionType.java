package com.tongguo.constant;

public final class PortionType {

    public static final String FULL = "FULL";
    public static final String HALF = "HALF";

    private PortionType() {
    }

    public static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return FULL;
        }
        String normalized = value.trim().toUpperCase();
        if (FULL.equals(normalized) || HALF.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("Dish portion type is invalid");
    }

    public static String normalizeStored(String value) {
        if (value == null || value.trim().isEmpty()) {
            return FULL;
        }
        String normalized = value.trim().toUpperCase();
        return HALF.equals(normalized) ? HALF : FULL;
    }

    public static boolean isHalf(String value) {
        return HALF.equals(normalize(value));
    }

    public static boolean isHalfStored(String value) {
        return HALF.equals(normalizeStored(value));
    }

    public static String displaySuffix(String value) {
        return isHalfStored(value) ? "（半份）" : "";
    }
}
