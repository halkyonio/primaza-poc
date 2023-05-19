package io.halkyon;

import static io.halkyon.utils.TestUtils.createClaim;
import static io.halkyon.utils.TestUtils.createService;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.test.junit.DisabledOnIntegrationTest;
import jakarta.ws.rs.core.MediaType;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.halkyon.model.Claim;
import io.halkyon.utils.WebPageExtension;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import io.restassured.specification.RequestSpecification;

@QuarkusTest
@QuarkusTestResource(WebPageExtension.class)
public class ClaimsEndpointTest {

    WebPageExtension.PageManager page;

    @Test
    public void testQueryClaimBody() {
        RequestSpecification httpRequest = RestAssured.given().header("HX-Request", "true").queryParam("name",
                "mysql-demo");
        Response response = httpRequest.get("/claims/filter");
        ResponseBody body = response.getBody();
        MatcherAssert.assertThat(body, Matchers.notNullValue());
    }

    @Test
    public void testQueryUsingNameToGetClaims() {
        final String claimName = "testQueryUsingNameToGetClaims";
        createService(claimName + "-service", "14.5", "type");
        createClaim(claimName, claimName + "-service-14.5");
        given().header("HX-Request", "true").queryParam("name", claimName).when().get("/claims/filter").then()
                .body(containsString("<td>" + claimName + "</td>"));
    }

    @Test
    @DisabledOnIntegrationTest
    public void testQueryUsingServiceRequestedToGetClaims() {
        final String claimName = "testQueryUsingServiceRequestedToGetClaims";
        createService(claimName + "-service", "14.5", "type");
        createClaim(claimName, claimName + "-service-14.5");
        given().header("HX-Request", "true")
                .queryParam("servicerequested", "testQueryUsingServiceRequestedToGetClaims-service").when()
                .get("/claims/filter").then().body(containsString("<td>" + claimName + "</td>"));
    }

    @Test
    public void claimCreatedViaForm() {
        // An htmx request will contain a HX-Request header and Content-Type: application/x-www-form-urlencoded
        String claimName = "mysql-claim";
        createService("mysql", "8.0.3", "type");
        given().header("HX-Request", true).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .formParam("name", claimName).formParam("serviceRequested", "mysql-8.0.3")
                .formParam("description", "mysql claim for testing purposes").when().post("/claims").then()
                .statusCode(201);

        Claim claim = given().contentType(MediaType.APPLICATION_JSON).get("/claims/name/" + claimName).then()
                .statusCode(200).extract().as(Claim.class);

        assertNotNull(claim);
        assertEquals(claim.name, claimName);
        assertEquals(claim.serviceRequested, "mysql-8.0.3");
        assertNotNull(claim.created);
    }

    @Test
    public void testDeleteUnBoundClaim() {
        final String claimName = "testDeleteBoundClaim";
        createService(claimName + "-service", "14.5", "type");
        Claim claim = createClaim(claimName, claimName + "-service-14.5");
        given().contentType(MediaType.APPLICATION_JSON).get("/claims/name/" + claimName).then().statusCode(200)
                .extract().as(Claim.class);

        given().header("HX-Request", "true").contentType(MediaType.APPLICATION_FORM_URLENCODED).when()
                .delete("/claims/" + claim.id).then().statusCode(200);
    }

    @Test
    public void testDeleteClaim() {
        String claimName = "ClaimsEndpointTest-testDeleteClaim";
        createService(claimName + "-service", "14.5", "type");
        Claim claim = createClaim(claimName, claimName + "-service-14.5");

        // When, we go to the claims page
        page.goTo("/claims");
        page.assertContentContains(claim.name);

        // And click on delete
        page.clickById("btn-claim-delete-" + claim.id);

        // Then, the claim should have been deleted
        page.goTo("/claims");
        page.assertContentDoesNotContain(claim.name);
    }

}
