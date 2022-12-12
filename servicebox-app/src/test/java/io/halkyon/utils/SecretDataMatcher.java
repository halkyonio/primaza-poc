package io.halkyon.utils;

import static io.halkyon.services.BindApplicationService.PASSWORD_KEY;
import static io.halkyon.services.BindApplicationService.URL_KEY;
import static io.halkyon.services.BindApplicationService.USERNAME_KEY;
import static io.halkyon.utils.StringUtils.toBase64;

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
        return Objects.equals(toBase64(url), data.get(URL_KEY))
                && Objects.equals(toBase64(username), data.get(USERNAME_KEY))
                && Objects.equals(toBase64(password), data.get(PASSWORD_KEY));
    }
}
