package io.halkyon;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.halkyon.utils.WebPageExtension;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(WebPageExtension.class)
public class HomeResourceTest {

    WebPageExtension.PageManager page;

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)
             .body(is("Hello from Primaza Service Box app"));
    }

    @Test
    public void testShouldRedirectToHome() {
        page.goTo("/");
        page.assertPathIs("/home");
    }

    @Test
    public void testShouldGoToClaimsPage() {
        page.goTo("/");
        page.clickOn("home-claims-btn-id");
        page.assertPathIs("/claims");
        // New button
        page.assertContentContains("New claim");
        // Go back button
        page.assertContentContains("Home");
    }

    @Test
    public void testShouldGoToServicesPage() {
        page.goTo("/");
        page.clickOn("home-services-btn-id");
        page.assertPathIs("/services");
        // New button
        page.assertContentContains("New service");
        // Go back button
        page.assertContentContains("Home");
    }

    @Test
    public void testShouldGoToClustersPage() {
        page.goTo("/");
        page.clickOn("home-clusters-btn-id");
        page.assertPathIs("/clusters");
        // New button
        page.assertContentContains("New cluster");
        // Go back button
        page.assertContentContains("Home");
    }

    @Test
    public void testShouldGoToCredentialsPage() {
        page.goTo("/");
        page.clickOn("home-credentials-btn-id");
        page.assertPathIs("/credentials");
        // New button
        page.assertContentContains("New credential");
        // Go back button
        page.assertContentContains("Home");
    }

    @Test
    public void testShouldGoToDeployedServicesPage() {
        page.goTo("/");
        page.clickOn("home-deployed-services-btn-id");
        page.assertPathIs("/services/discovered");
        // Go back button
        page.assertContentContains("Home");
    }
}