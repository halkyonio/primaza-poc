package io.halkyon;

import static io.halkyon.utils.TestUtils.createCluster;
import static io.halkyon.utils.TestUtils.createService;
import static io.halkyon.utils.TestUtils.mockServiceIsAvailableInCluster;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;

import io.halkyon.model.Service;
import io.halkyon.services.KubernetesClientService;
import io.halkyon.utils.WebPageExtension;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.DisabledOnIntegrationTest;
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
        // An htmx request will contain a HX-Request header and Content-Type: application/x-www-form-urlencoded

        String rabbitMQ4 = "RabbitMQ4";
        given()
                .header("HX-Request", true)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .formParam("name", rabbitMQ4)
                .formParam("version", "3.11.2")
                .formParam("type", "Broker")
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

    @Test
    public void testCannotAddServiceWithSameNameAndVersion() {
        String serviceName = "RabbitMQ2";
        String serviceVersion = "3.11.0";
        String serviceType= "Broker";
        String endpoint = "tcp:5672";
        String database = "demo";

        createService(serviceName, serviceVersion, serviceType, database, endpoint);

        given()
                .header("HX-Request", true)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .formParam("name", serviceName)
                .formParam("version", serviceVersion)
                .formParam("type", serviceType)
                .formParam("endpoint", endpoint)
                .when().post("/services")
                .then().statusCode(409);
    }

    @Test
    public void testDeleteServiceInPage() {
        // First, we create a cluster with a service
        String prefix = "ServicesEndpointTest-testDeleteServiceInPage-";
        Service service = createService(prefix + "service", "Api", "any", "demo");

        // When, we go to the services page
        page.goTo("/services");
        page.assertContentContains(service.name);

        // And click on delete
        page.clickById("btn-service-delete-" + service.id);

        // Then, the service should have been deleted
        page.goTo("/services");
        page.assertContentDoesNotContain(service.name);
    }

    /**
     * `@InjectMock` does not work when running tests in prod mode.
     */
    @DisabledOnIntegrationTest
    @Test
    public void testDeleteServiceWithClusterInPage() {
        // First, we create a cluster with a service
        String prefix = "ServicesEndpointTest-testDeleteServiceWithClusterInPage-";
        String clusterName = prefix + "cluster";
        createCluster(clusterName, "master:port");
        mockServiceIsAvailableInCluster(mockKubernetesClientService, clusterName, "testDeleteClusterInPage", "1111", "ns1");
        Service service = createService(prefix + "service", "Api", "any", "demo", "testDeleteClusterInPage:1111");

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
