package io.halkyon;

import io.halkyon.model.Claim;
import io.halkyon.model.Service;
import io.halkyon.utils.TestUtils;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import static io.halkyon.utils.TestUtils.createService;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;
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
                .formParam("name", "RabbitMQ2")
                .formParam("version", "3.11.2")
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

    public void serviceIsCreatedViaForm() {
        // An htmx request will contain a HX-Request header and Content-Type: application/x-www-form-urlencoded
        String rabbitMQ4 = "RabbitMQ4";
        given()
                .header("HX-Request", true)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .formParam("name", rabbitMQ4)
                .formParam("version", "3.11.1")
                .formParam("endpoint", "tcp:5672")
                .when().post("/services")
                .then().statusCode(201);
    }

//    @Test
    //TODO This test doesn't work passing by the ServiceResource#addmethod because the services it tries to persist the service twice in the same transaction and a primary key violation constrain occurs
    public void testCannotAddServiceWithSameNameAndVersion() {
        String serviceName = "RabbitMQ4";
        String serviceVersion = "3.11.2";
        String endpoint = "tcp:5672";

        createService(serviceName, serviceVersion, endpoint, false);

        given()
                .header("HX-Request", true)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .formParam("name", serviceName)
                .formParam("version", serviceVersion)
                .formParam("endpoint", endpoint)
                .when().post("/services")
                .then().statusCode(409);
    }

}
