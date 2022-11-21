package io.halkyon.qute;

import io.halkyon.ApplicationProperties;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
