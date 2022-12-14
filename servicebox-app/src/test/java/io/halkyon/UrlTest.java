package io.halkyon;

import static io.halkyon.utils.StringUtils.removeSchemeFromUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class UrlTest {

    @Test
    public void checkHostPortFromUrl() {
        String url = removeSchemeFromUrl("tcp://postgresql.db:5432");
        assertEquals("postgresql.db:5432", url);
    }
}
