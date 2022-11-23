package io.halkyon.utils;

import static io.restassured.RestAssured.given;

import javax.transaction.Transactional;
import javax.ws.rs.core.MediaType;

import io.halkyon.model.Claim;
import io.halkyon.model.Cluster;
import io.halkyon.model.Service;

public final class TestUtils {
    private TestUtils() {

    }

    public static Cluster createCluster(String clusterName, String url) {
        given()
                .header("HX-Request", true)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .multiPart("name", clusterName)
                .multiPart("environment","PROD")
                .multiPart("url", url)
                .when().post("/clusters")
                .then().statusCode(201);

        return given()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/clusters/name/" + clusterName)
                .then()
                .statusCode(200)
                .extract().as(Cluster.class);
    }

    public static Service createService(String serviceName, String serviceVersion, boolean available) {
        return createService(serviceName, serviceVersion, "tcp:5672", available);
    }

    public static Service createService(String serviceName, String serviceVersion, String endpoint) {
        return createService(serviceName, serviceVersion, endpoint, false);
    }

    @Transactional
    public static Service createService(String serviceName, String serviceVersion, String endpoint, boolean available) {
        given()
                .header("HX-Request", true)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .formParam("name",serviceName)
                .formParam("version",serviceVersion)
                .formParam("endpoint", endpoint )
                .when().post("/services")
                .then().statusCode(201);

        Service service = given()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/services/name/" + serviceName)
                .then()
                .statusCode(200)
                .extract().as(Service.class);

        if (available){
            Service.update("update from Service set available = true where id = ?1", service.id);
        }
        return service;
    }

    public static Claim createClaim(String claimName, String serviceRequested) {
        given()
                .header("HX-Request", true)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .formParam("name",claimName)
                .formParam("serviceRequested",serviceRequested)
                .formParam("status","new")
                .formParam("description","claim for testing purposes")
                .when().post("/claims")
                .then()
                .statusCode(201);
        return given()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/claims/name/" + claimName)
                .then()
                .statusCode(200)
                .extract().as(Claim.class);

    }

    public static void createCredential(String credentialName, long serviceId, String username, String password) {
        given()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .formParam("name", credentialName)
                .formParam("serviceId", serviceId)
                .formParam("username", username)
                .formParam("password", password)
                .when().post("/credentials")
                .then().statusCode(201);
    }
}
