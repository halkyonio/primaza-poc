package io.halkyon;

import io.halkyon.model.Claim;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import io.restassured.specification.RequestSpecification;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import static io.halkyon.utils.TestUtils.createClaim;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class ClaimsEndpointTest {

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
        given().header("HX-Request","true").queryParam("servicerequested","Postgresql-5509")
               .when().get("/claims/filter")
               .then().body(containsString("<td>" + claimName + "</td>"));
    }

    @Test
    public void claimCreatedViaForm(){
        // An htmx request will contain a HX-Request header and Content-Type: application/x-www-form-urlencoded
        String claimName = "mysql-claim";
        given()
                .header("HX-Request", true)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .formParam("name", claimName)
                .formParam("serviceRequested","mysql-8.0.3")
                .formParam("description","mysql claim for testing purposes")
                .when().post("/claims")
                .then()
                .statusCode(201);

        Claim claim = given()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/claims/name/" + claimName)
                .then()
                .statusCode(200)
                .extract().as(Claim.class);

        assertNotNull(claim);
        assertEquals(claim.name,claimName);
        assertEquals(claim.serviceRequested, "mysql-8.0.3");
        assertNotNull(claim.created);
    }

}
