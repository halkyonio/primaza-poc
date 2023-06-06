package io.halkyon;

import static io.halkyon.utils.TestUtils.createCluster;
import static io.halkyon.utils.TestUtils.createService;
import static io.halkyon.utils.TestUtils.mockServiceIsAvailableInCluster;
import static io.restassured.RestAssured.given;

import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;

import io.halkyon.model.Cluster;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ClustersEndpointTest extends BaseTest {

    @Test
    public void testAddClusterViaHtmxForm() {
        given().contentType(MediaType.MULTIPART_FORM_DATA).multiPart("name", "ocp4.11-node-2")
                .multiPart("environment", "TEST").multiPart("excludedNamespaces", "kube-system,ingress")
                .multiPart("url", "https://10.0.2.12:6443").when().post("/clusters").then().statusCode(201);
    }

    @Test
    public void testEditClusterFromPage() {
        // Create data
        String prefix = "ClustersEndpointTest-testEditClusterFromPage-";
        Cluster cluster = createCluster(prefix + "cluster", "master:9999");
        // Go to the clusters page
        page.goTo("/clusters");
        // Ensure our data is listed
        page.assertContentContains(cluster.name);
        // Let's change the owner
        page.clickById("btn-cluster-edit-" + cluster.id);
        page.assertPathIs("/clusters/" + cluster.id);
        page.assertContentContains("Update Cluster");
        page.assertContentContains(cluster.name);
        page.type("clusterName", cluster.name + "-new");
        page.clickById("cluster-button");
        // Verify the entity was properly updated:
        page.assertContentContains("Updated successfully for id: " + cluster.id);
        // Go back to the clusters list and check whether the owner is displayed
        page.goTo("/clusters");
        page.assertContentContains(cluster.name + "-new");
    }

    @Test
    public void testDeleteClusterInPage() {
        // First, we create a cluster with a service
        String prefix = "ClustersEndpointTest-testDeleteClusterInPage-";
        Cluster cluster = createCluster(prefix + "cluster", "master:9999");
        mockServiceIsAvailableInCluster(mockKubernetesClientService, cluster.name, "testDeleteClusterInPage", "1111",
                "ns1");
        createService(prefix + "service", "Api", "any", "testDeleteClusterInPage:1111");

        // When, we go to the clusters page
        page.goTo("/clusters");
        page.assertContentContains(cluster.name);

        // And click on delete
        page.clickById("btn-cluster-delete-" + cluster.id);

        // Then, the cluster should have been deleted
        page.goTo("/clusters");
        page.assertContentDoesNotContain(cluster.name);
    }

}
