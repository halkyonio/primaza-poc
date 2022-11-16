package io.halkyon.utils;

import static io.restassured.RestAssured.given;

import javax.ws.rs.core.MediaType;

import io.halkyon.model.Claim;
import io.halkyon.model.Cluster;
import io.halkyon.model.Service;

public final class TestUtils {
    private TestUtils() {

    }

    public static Cluster createCluster(String clusterName, String url) {
        return given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/json")
                .body("{\"name\": \"" + clusterName + "\", "
                        + "\"environment\": \"PROD\", "
                        + "\"url\": \"" + url + "\" }")
                .when().post("/clusters")
                .then().statusCode(201)
                .extract().as(Cluster.class);
    }

    public static Service createService(String serviceName, String serviceVersion, boolean deployed) {
        return createService(serviceName, serviceVersion, "tcp:5672", deployed);
    }

    public static Service createService(String serviceName, String serviceVersion, String endpoint) {
        return createService(serviceName, serviceVersion, endpoint, false);
    }

    public static Service createService(String serviceName, String serviceVersion, String endpoint, boolean deployed) {
        return given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/json")
                .body("{\"name\": \"" + serviceName + "\", "
                        + "\"version\": \"" + serviceVersion + "\", "
                        + "\"endpoint\": \"" + endpoint + "\", "
                        + "\"deployed\": \"" + deployed + "\" }")
                .when().post("/services")
                .then().statusCode(201)
                .extract().as(Service.class);
    }

    public static Claim createClaim(String claimName, String serviceRequested) {
        return given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/json")
                .body("{\"name\": \"" + claimName + "\", "
                        + "\"serviceRequested\": \"" + serviceRequested + "\", "
                        + "\"status\": \"new\"}")
                .when().post("/claims")
                .then().statusCode(201)
                .extract().as(Claim.class);
    }
}
