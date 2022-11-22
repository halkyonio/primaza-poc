package io.halkyon.utils;

import java.util.Objects;

import org.mockito.ArgumentMatcher;

import io.halkyon.model.Application;

public class ApplicationNameMatcher implements ArgumentMatcher<Application> {

    private final String appName;

    public ApplicationNameMatcher(String appName) {
        this.appName = appName;
    }

    @Override
    public boolean matches(Application application) {
        if (application == null) {
            return false;
        }

        return Objects.equals(appName, application.name);
    }
}
