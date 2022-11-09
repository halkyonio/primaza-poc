package io.halkyon.utils;

import static io.restassured.RestAssured.given;

import javax.ws.rs.core.MediaType;

public final class TestUtils {
    private TestUtils() {

    }

    public static void createService(String serviceName, String serviceVersion, boolean deployed) {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/json")
                .body("{\"name\": \"" + serviceName + "\", "
                        + "\"version\": \"" + serviceVersion + "\", "
                        + "\"endpoint\": \"tcp:5672\", "
                        + "\"deployed\": \"" + deployed + "\" }")
                .when().post("/services");
    }
}
