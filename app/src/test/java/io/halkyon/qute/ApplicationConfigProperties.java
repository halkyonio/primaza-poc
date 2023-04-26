package io.halkyon.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.halkyon.ApplicationProperties;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ApplicationConfigProperties {
    @Inject
    ApplicationProperties applicationProperties;

    @Test
    public void testInjectionAndDefaultValues() {
        assertEquals("http://github.com/halkyonio/primaza-poc", applicationProperties.getGitHubRepo());
        assertEquals("666", applicationProperties.getGitShaCommit());
    }

}
