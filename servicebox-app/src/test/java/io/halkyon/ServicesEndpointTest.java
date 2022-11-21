package io.halkyon;

import io.halkyon.model.Service;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

@QuarkusTest
public class ServicesEndpointTest {

    @Test
    public void testAddService(){
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/json")
                .body("{\"name\": \"RabbitMQ\", \"version\": \"3.11.2\", \"endpoint\": \"tcp:5672\", \"available\": \"false\" }")
                .when().post("/services")
                .then()
                .statusCode(201);

    }

    @Test
    public void testAddServiceViaHtmxForm(){
        // An htmx request will contain a HX-Request header and Content-Type: application/x-www-form-urlencoded
        given()
                .header("HX-Request", true)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("{\"name\": \"RabbitMQ2\", \"version\": \"3.11.2\", \"endpoint\": \"tcp:5672\", \"available\": \"false\" }")
                .when().post("/services")
                .then()
                .statusCode(201);
    }

    @Test
    public void testServiceEntity() {
        final String path="/services";
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/json")
                .body("{\"name\": \"RabbitMQ3\", \"version\": \"3.11.2\", \"endpoint\": \"tcp:5672\", \"available\": \"false\" }")
                .when().post("/services")
                .then()
                .statusCode(201);

        Service service = given()
                .when().get("/services/name/RabbitMQ3")
                .then()
                .statusCode(200)
                .extract().as(Service.class);

        //Delete the 'RabbitMQ':
        given()
                .when().delete(path + "/" + service.id)
                .then()
                .statusCode(204);

        //List all, 'RabbitMQ3' should be missing now:
        given()
                .when().get(path)
                .then()
                .statusCode(200)
                .body(not(containsString("RabbitMQ3")));
    }

    @Test
    public void testCannotAddServiceWithSameNameAndVersion(){
        String request = "{\"name\": \"RabbitMQ4\", \"version\": \"3.11.2\", \"endpoint\": \"tcp:5672\", \"available\": \"false\" }";
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/json")
                .body(request)
                .when().post("/services")
                .then()
                .statusCode(201);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/json")
                .body(request)
                .when().post("/services")
                .then()
                .statusCode(409);
    }

}
