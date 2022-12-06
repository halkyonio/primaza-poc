package io.halkyon;

import io.halkyon.model.Service;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import static io.halkyon.utils.TestUtils.createService;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class ServicesEndpointTest {

    @Test
    public void serviceIsFoundByName() {
        // An htmx request will contain a HX-Request header and Content-Type: application/x-www-form-urlencoded

        String rabbitMQ4 = "RabbitMQ4";
        given()
                .header("HX-Request", true)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .formParam("name", rabbitMQ4)
                .formParam("version", "3.11.2")
                .formParam("type", "Broker")
                .formParam("endpoint", "tcp:5672")
                .when().post("/services")
                .then().statusCode(201);

        Service service = given()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/services/name/" + rabbitMQ4)
                .then()
                .statusCode(200)
                .extract().as(Service.class);

        assertNotNull(service);
        assertEquals(service.name, rabbitMQ4);
        assertNotNull(service.created);

    }

    @Test
    public void testCannotAddServiceWithSameNameAndVersion() {
        String serviceName = "RabbitMQ2";
        String serviceVersion = "3.11.0";
        String serviceType= "Broker";
        String endpoint = "tcp:5672";
        String database = "demo";

        createService(serviceName, serviceVersion, serviceType, database, endpoint, false);

        given()
                .header("HX-Request", true)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .formParam("name", serviceName)
                .formParam("version", serviceVersion)
                .formParam("type", serviceType)
                .formParam("endpoint", endpoint)
                .when().post("/services")
                .then().statusCode(409);
    }

}
