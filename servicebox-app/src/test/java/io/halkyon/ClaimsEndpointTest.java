package io.halkyon;

import static io.halkyon.utils.TestUtils.createClaim;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import javax.ws.rs.core.MediaType;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import io.restassured.specification.RequestSpecification;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.halkyon.model.Claim;
import io.quarkus.test.junit.QuarkusTest;

import java.util.regex.Matcher;

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
    public void testQueryClaimBody(){
         RequestSpecification httpRequest = RestAssured.given().header("HX-Request","true").queryParam("name","mysql-demo");
         Response response = httpRequest.get("/claims/filter");
         ResponseBody body = response.getBody();
         MatcherAssert.assertThat(body,Matchers.notNullValue());
    }
    @Test
    public void testQueryUsingNameToGetClaims(){
        final String claimName = "testQueryUsingNameToGetClaims";
        createClaim(claimName, "Postgresql-5509");
        given().header("HX-Request","true")
               .queryParam("name",claimName)
               .when()
                 .get("/claims/filter")
               .then()
                 .body(containsString("<td>" + claimName + "</td>"));
    }

    @Test
    public void testQueryUsingServiceRequestedToGetClaims(){
        final String claimName = "testQueryUsingServiceRequestedToGetClaims";
        createClaim(claimName, "Postgresql-5509");
        given().queryParam("servicerequested","Postgresql-5509")
               .when().get("/claims/filter")
               .then().body(containsString("<td>" + claimName + "</td>"));
    }

    @Test
    public void testAddClaimViaHtmxForm(){
        // An htmx request will contain a HX-Request header and Content-Type: application/x-www-form-urlencoded
        given()
                .header("HX-Request", true)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .formParam("name", "ClaimsEndpointTest.testAddClaimViaHtmxForm")
                .formParam("serviceRequested", "mysql:3306")
                .when().post("/claims")
                .then()
                .statusCode(201);

        // TODO check the created date is not null
    }

    @Test
    public void testClaimEntity() {
        final String claimName = "testClaimEntity";
        createClaim(claimName, "Postgresql-5509");

        Claim claim = given()
                .when().get("/claims/name/" + claimName)
                .then()
                .statusCode(200)
                .extract().as(Claim.class);

        //Delete the 'Postgresql':
        given()
                .when().delete("/claims/" + claim.id)
                .then()
                .statusCode(204);

        //List all, 'Postgresql' should be missing now:
        given()
                .when().get("/claims")
                .then()
                .statusCode(200)
                .body(not(containsString(claimName)));
    }

}
