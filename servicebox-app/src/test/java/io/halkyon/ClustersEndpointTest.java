package io.halkyon;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClustersEndpointTest {

    @Order(1)
    @Test
    public void testAddCluster(){
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/json")
                .body("{\"name\": \"ocp4.11-node-2\", \"environment\": \"PROD\", \"url\": \"https://10.0.2.11:6443\" }")
                .when().post("/clusters")
                .then()
                .statusCode(201);

    }

    @Order(2)
    @Test
    public void testAddClusterViaHtmxForm(){
        // An htmx request will contain a HX-Request header and Content-Type: application/x-www-form-urlencoded
        given()
                .header("HX-Request", true)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("{\"name\": \"ocp4.11-node-3\", \"environment\": \"TEST\", \"url\": \"https://10.0.2.12:6443\" }")
                .when().post("/clusters")
                .then()
                .statusCode(201);
    }

    @Order(3)
    @Test
    public void testClusterEntity() {
        final String path="/clusters";
        //List all
        given()
                .when().get(path)
                .then()
                .statusCode(200)
                .body(containsString("ocp4.11-node-1"));

        //Delete the 'ocp4.11-node-1':
        given()
                .when().delete(path + "/1")
                .then()
                .statusCode(204);

        //List all, 'ocp4.11-node-1' should be missing now:
        given()
                .when().get(path)
                .then()
                .statusCode(200)
                .body(not(containsString("ocp4.11-node-1")));
    }

}
