package io.halkyon;

import org.junit.jupiter.api.Test;

import static io.halkyon.utils.StringUtils.removeSchemeFromUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UrlTest {

    @Test
    public void checkHostPortFromUrl() {
        String url = removeSchemeFromUrl("tcp://postgresql.db:5432");
        assertEquals("postgresql.db:5432",url);
    }
}
