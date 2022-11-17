package io.halkyon.utils;

public final class StringUtils {
    private StringUtils() {

    }

    public static boolean equalsIgnoreCase(String left, String right) {
        if (left == null && right == null) {
            return true;
        } else if (left == null) {
            return false;
        } else if (right == null) {
            return false;
        } else {
            return left.equalsIgnoreCase(right);
        }
    }
}
