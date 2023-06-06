package io.halkyon;

import static io.halkyon.utils.TestUtils.createCluster;
import static io.halkyon.utils.TestUtils.createService;
import static io.halkyon.utils.TestUtils.mockServiceIsAvailableInCluster;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;

import io.halkyon.model.Service;
import io.halkyon.services.KubernetesClientService;
import io.halkyon.utils.WebPageExtension;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
@QuarkusTestResource(WebPageExtension.class)
public class ServicesEndpointTest {

    WebPageExtension.PageManager page;

    @InjectMock
    KubernetesClientService mockKubernetesClientService;

    @Test
    public void serviceIsFoundByName() {
        String rabbitMQ4 = "RabbitMQ4";
        given().contentType(MediaType.APPLICATION_FORM_URLENCODED).formParam("name", rabbitMQ4)
                .formParam("version", "3.11.2").formParam("type", "Broker").formParam("endpoint", "tcp:5672").when()
                .post("/services").then().statusCode(201);

        Service service = given().contentType(MediaType.APPLICATION_JSON).get("/services/name/" + rabbitMQ4).then()
                .statusCode(200).extract().as(Service.class);

        assertNotNull(service);
        assertEquals(service.name, rabbitMQ4);
        assertNotNull(service.created);
    }

    @Test
    public void testCannotAddServiceWithSameNameAndVersion() {
        String prefix = "ServicesEndpointTest-testCannotAddServiceWithSameNameAndVersion-";
        Service service = createService(prefix + "service", "1", "type");

        given().contentType(MediaType.APPLICATION_FORM_URLENCODED).formParam("name", service.name)
                .formParam("version", service.version).formParam("type", service.type)
                .formParam("endpoint", service.endpoint).when().post("/services").then().statusCode(409);
    }

    @Test
    public void testCannotUpdateServiceWithSameNameAndVersion() {
        String prefix = "ServicesEndpointTest-testCannotUpdateServiceWithSameNameAndVersion-";
        Service service1 = createService(prefix + "service", "1", "type");
        Service service2 = createService(prefix + "service", "2", "type");

        given().contentType(MediaType.APPLICATION_FORM_URLENCODED).formParam("name", service2.name)
                .formParam("version", "1") // conflicts with version of service1!
                .formParam("type", service2.type).formParam("endpoint", service2.endpoint).when()
                .put("/services/" + service2.id).then().statusCode(409);
    }

    @Test
    public void testEditServiceFromPage() {
        // Create data
        String prefix = "ServicesEndpointTest-testEditServiceFromPage-";
        Service service = createService(prefix + "service", "master:port", "type");
        // Go to the page
        page.goTo("/services");
        // Ensure our data is listed
        page.assertContentContains(service.name);
        // Let's change the owner
        page.clickById("btn-service-edit-" + service.id);
        page.assertPathIs("/services/" + service.id);
        page.assertContentContains("Update Service");
        page.assertContentContains(service.name);
        page.type("name", service.name + "-new");
        page.clickById("service-button");
        // Verify the entity was properly updated:
        page.assertContentContains("Updated successfully for id: " + service.id);
        // Go back to the page list and check whether the owner is displayed
        page.goTo("/services");
        page.assertContentContains(service.name + "-new");
    }

    @Test
    public void testDeleteServiceInPage() {
        // First, we create a cluster with a service
        String prefix = "ServicesEndpointTest-testDeleteServiceInPage-";
        Service service = createService(prefix + "service", "Api", "any");

        // When, we go to the services page
        page.goTo("/services");
        page.assertContentContains(service.name);

        // And click on delete
        page.clickById("btn-service-delete-" + service.id);

        // Then, the service should have been deleted
        page.goTo("/services");
        page.assertContentDoesNotContain(service.name);
    }

    @Test
    public void createStandaloneService() {
        String prefix = "ServicesEndpointTest-createStandaloneService-";
        String serviceName = prefix + "service";
        given().contentType(MediaType.APPLICATION_FORM_URLENCODED).formParam("name", serviceName)
                .formParam("version", "3.11.2").formParam("type", "Broker").formParam("endpoint", "tcp:5672")
                .formParam("externalEndpoint", "rabbit.com").when().post("/services").then().statusCode(201);

        Service service = given().contentType(MediaType.APPLICATION_JSON).get("/services/name/" + serviceName).then()
                .statusCode(200).extract().as(Service.class);

        assertNotNull(service);
        assertTrue(service.available);
        assertNotNull(service.created);

        // This shouldn't be checked here but in ServiceDiscoveryJobTest or something like this
        // And also a standalone service is not in a cluster (cluster=null) so it can't be discovered
        // Then, the service should be listed in the discovered page
        // page.goTo("/services/discovered");
        // page.assertContentContains(serviceName);
        // page.assertContentContains("rabbit.com");
    }

    @Test
    public void testDeleteServiceWithClusterInPage() {
        // First, we create a cluster with a service
        String prefix = "ServicesEndpointTest-testDeleteServiceWithClusterInPage-";
        String clusterName = prefix + "cluster";
        createCluster(clusterName, "master:9999");
        mockServiceIsAvailableInCluster(mockKubernetesClientService, clusterName, "testDeleteClusterInPage", "1111",
                "ns1");
        Service service = createService(prefix + "service", "Api", "any", "testDeleteClusterInPage:1111");

        // When, we go to the services page
        page.goTo("/services");
        page.assertContentContains(service.name);

        // And click on delete
        page.clickById("btn-service-delete-" + service.id);

        // Then, the service should have been deleted
        page.goTo("/services");
        page.assertContentDoesNotContain(service.name);
    }

}
