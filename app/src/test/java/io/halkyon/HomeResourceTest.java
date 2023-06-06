package io.halkyon;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class HomeResourceTest extends BaseTest {

    @Test
    public void testShouldRedirectToHome() {
        page.goTo("/");
        page.assertPathIs("/home");
    }

    @Test
    public void testShouldGoToClaimsPage() {
        page.goTo("/");
        page.clickById("home-claims-btn-id");
        page.assertPathIs("/claims");
        // New button
        page.assertContentContains("New claim");
        // Go back button
        page.assertContentContains("Home");
    }

    @Test
    public void testShouldGoToServicesPage() {
        page.goTo("/");
        page.clickById("home-services-btn-id");
        page.assertPathIs("/services");
        // New button
        page.assertContentContains("New service");
        // Go back button
        page.assertContentContains("Home");
    }

    @Test
    public void testShouldGoToClustersPage() {
        page.goTo("/");
        page.clickById("home-clusters-btn-id");
        page.assertPathIs("/clusters");
        // New button
        page.assertContentContains("New cluster");
        // Go back button
        page.assertContentContains("Home");
    }

    @Test
    public void testShouldGoToCredentialsPage() {
        page.goTo("/");
        page.clickById("home-credentials-btn-id");
        page.assertPathIs("/credentials");
        // New button
        page.assertContentContains("New credential");
        // Go back button
        page.assertContentContains("Home");
    }

    @Test
    public void testShouldGoToAvailableServicesPage() {
        page.goTo("/");
        page.clickById("home-available-services-btn-id");
        page.assertPathIs("/services/discovered");
        // Go back button
        page.assertContentContains("Home");
    }

    @Test
    public void testShouldGoToApplicationsPage() {
        page.goTo("/");
        page.clickById("home-applications-btn-id");
        page.assertPathIs("/applications");
        // Go back button
        page.assertContentContains("Home");
    }
}