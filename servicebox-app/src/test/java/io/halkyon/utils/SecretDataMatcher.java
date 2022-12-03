package io.halkyon.utils;

import java.util.Map;
import java.util.Objects;

import org.mockito.ArgumentMatcher;

public class SecretDataMatcher  implements ArgumentMatcher<Map<String, String>> {

    private final String uri;
    private final String username;
    private final String password;
    private final String type;


    public SecretDataMatcher(String uri, String username, String password, String type) {
        this.uri = uri;
        this.username = username;
        this.password = password;
        this.type= type;
    }

    @Override
    public boolean matches(Map<String, String> data) {
        return Objects.equals(uri, data.get("uri"))
                && Objects.equals(username, data.get("username"))
                && Objects.equals(password, data.get("password"))
                && Objects.equals(type, data.get("type"));
    }
}
