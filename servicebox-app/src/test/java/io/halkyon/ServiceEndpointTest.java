package io.halkyon;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsNot.not;

@QuarkusTest
public class ServiceEndpointTest {


    @Test
    public void testAddService(){

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/json")
                .body("{\"name\": \"RabbitMQ\", \"version\": \"3.11.2\", \"endpoint\": \"tcp:5672\", \"deployed\": \"false\" }")
                .when().post("/service")
                .then()
                .statusCode(201);

    }

    @Test
    public void testAddServiceViaHtmxForm(){
        // An htmx request will contain a HX-Request header and Content-Type: application/x-www-form-urlencoded
        given()
                .header("HX-Request", true)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("{\"name\": \"RabbitMQ\", \"version\": \"3.11.2\", \"endpoint\": \"tcp:5672\", \"deployed\": \"false\" }")
                .when().post("/service")
                .then()
                .statusCode(201);
    }

    @Test
    public void testFindByName(){

        given()
                .when().get("/service")
                .then()
                .statusCode(200)
                .body(
                        containsString("MYSQL"),
                        containsString("PostgreSQL"),
                        containsString("ActiveMQ Artemis"),
                        containsString("PaymentAPI"));

        given()
                .when().get("/service/MYSQL")
                .then()
                .statusCode(200)
                .body(containsString("MYSQL"));
    }

    @Test
    public void testServiceEntity() {
        final String path="/service";
        //List all
        given()
                .when().get(path)
                .then()
                .statusCode(200)
                .body(
                        containsString("MYSQL"),
                        containsString("PostgreSQL"),
                        containsString("ActiveMQ Artemis"),
                        containsString("PaymentAPI"));

        //Delete the 'mysql-demo':
        given()
                .when().delete(path + "/1")
                .then()
                .statusCode(204);

        //List all, 'mysql-demo' should be missing now:
        given()
                .when().get(path)
                .then()
                .statusCode(200)
                .body(
                        not(containsString("MYSQL")),
                        containsString("PostgreSQL"),
                        containsString("ActiveMQ Artemis"),
                        containsString("PaymentAPI"));
    }

}
