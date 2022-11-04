package io.halkyon;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsNot.not;

import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;

import io.halkyon.model.Claim;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ClaimsEndpointTest {

    @Test
    public void testAddClaim(){
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/json")
                .body("{\"name\": \"Oracle\", \"serviceRequested\": \"oracle-database\"}")
                .when().post("/claims")
                .then()
                .statusCode(201)
                .body(containsString("Oracle"))
                .body("id", notNullValue())
                .extract().body().jsonPath().getString("id");
    }

    @Test
    public void testAddClaimViaHtmxForm(){
        // An htmx request will contain a HX-Request header and Content-Type: application/x-www-form-urlencoded
        given()
                .header("HX-Request", true)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("{\"name\": \"mysql-demo\", \"serviceRequested\": \"mysql:3306\"}")
                .when().post("/claims")
                .then()
                .statusCode(201);

        // TODO check the created date is not null
    }

    @Test
    public void testClaimEntity() {
        final String path="/claims";
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/json")
                .body("{\"name\": \"Postgresql-testClaimEntity\", \"serviceRequested\": \"Postgresql-5509\"}")
                .when().post("/claims")
                .then()
                .statusCode(201);

        Claim claim = given()
                .when().get("/claims/name/Postgresql-testClaimEntity")
                .then()
                .statusCode(200)
                .extract().as(Claim.class);

        //Delete the 'Postgresql':
        given()
                .when().delete(path + "/" + claim.id)
                .then()
                .statusCode(204);

        //List all, 'Postgresql' should be missing now:
        given()
                .when().get(path)
                .then()
                .statusCode(200)
                .body(not(containsString("Postgresql-testClaimEntity")));
    }

}
