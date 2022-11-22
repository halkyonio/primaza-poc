package io.halkyon.utils;

import java.util.Map;
import java.util.Objects;

import org.mockito.ArgumentMatcher;

public class SecretDataMatcher  implements ArgumentMatcher<Map<String, String>> {

    private final String url;
    private final String username;
    private final String password;

    public SecretDataMatcher(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Override
    public boolean matches(Map<String, String> data) {
        return Objects.equals(url, data.get("url"))
                && Objects.equals(username, data.get("username"))
                && Objects.equals(password, data.get("password"));
    }
}
