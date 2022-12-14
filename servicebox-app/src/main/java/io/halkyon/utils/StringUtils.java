package io.halkyon.utils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class StringUtils {
    private StringUtils() {
    }

    public static String removeSchemeFromUrl(String URL) {
        URI uri = URI.create(URL);
        return uri.getHost() + ":" + String.valueOf(uri.getPort());
    }

    public static String getHostFromUrl(String URL) {
        URI uri = URI.create(URL);
        return uri.getHost();
    }

    public static String getPortFromUrl(String URL) {
        URI uri = URI.create(URL);
        return String.valueOf(uri.getPort());
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

    public static String toBase64(String paramValue) {
        return Base64.getEncoder().encodeToString(paramValue.getBytes(StandardCharsets.UTF_8));
    }
}
