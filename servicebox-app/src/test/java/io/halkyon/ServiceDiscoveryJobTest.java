package io.halkyon;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointAddressBuilder;
import io.fabric8.kubernetes.api.model.EndpointPortBuilder;
import io.fabric8.kubernetes.api.model.EndpointSubsetBuilder;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.halkyon.model.Claim;
import io.halkyon.model.Service;
import io.halkyon.services.ClaimStatus;
import io.halkyon.services.ClaimingJobService;
import io.halkyon.services.ServiceDiscoveryJob;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;

@WithKubernetesTestServer
@QuarkusTest
public class ServiceDiscoveryJobTest {

    KubernetesClient client;

    @KubernetesTestServer
    KubernetesServer mockServer;

    @Inject
    ServiceDiscoveryJob job;

    @Inject
    Scheduler scheduler;



    @Test
    public void shouldFindTheKubernetesServiceCorrespondingThePrimazaResgisteredService(){
        pauseScheduler();

        //Given a Postgresql service running in a Kubernetes cluster
        registerPostgresqlServiceinKubernetes();

        //Given 2 services registered in the DB, one of them the Postgresql service (representing the instance running in k8s)
        createPostgresqlService();
        createRabitMQService();

        given()
                .when().get("/services")
                .then()
                .statusCode(200)
                .body(containsString("RabbitMQ"),
                      containsString("PostgreSQL"));

        //When the job runs
        job.execute();


        //Then the rabbitMQ is deleted from DB because it is not running in the cluster
        given()
                .when().get("/services/name/RabbitMQ")
                .then()
                .statusCode(204)
                .body(not(containsString("RabbitMQ")));


        given()
                .when().get("/services/name/PostgreSQL")
                .then()
                .statusCode(200)
                .body(containsString("PostgreSQL"));


    }

    private void registerPostgresqlServiceinKubernetes() {

        Map<String, String> labels = new HashMap<>();
        labels.put("type","color");
        labels.put("app.kubernetes.io/version","1.0");

        final Pod pod = new PodBuilder().withNewSpec().endSpec().withNewMetadata().withName("PostgreSQL").withLabels(labels).and().build();

        //Since we are using a mockServer, we are not able to make any real application running in there, so we run it locally and configure the k8s endpoint to return `localhost`
        // as IP. This way, we will send the request to localhost where the RedService is actually running.
        String[] ips = { "localhost"};
        List<EndpointAddress> endpointAddresses = Arrays.stream(ips)
                .map(ipAddress -> {
                    String uid = UUID.randomUUID().toString();
                    ObjectReference targetRef = new ObjectReference(null, null, "Pod",
                            "PostgreSQL", "development", null, uid);
                    EndpointAddress endpointAddress = new EndpointAddressBuilder().withIp(ipAddress).withTargetRef(targetRef)
                            .build();
                    return endpointAddress;
                }).collect(Collectors.toList());

        Endpoints endpoint = new EndpointsBuilder()
                .withNewMetadata().withName("PostgreSQL").endMetadata()
                .addToSubsets(new EndpointSubsetBuilder().withAddresses(endpointAddresses)
                        .addToPorts(new EndpointPortBuilder().withPort(5432).withProtocol("TCP").build())
                        .build())
                .build();

        // Set up Kubernetes so that our "pretend" pods and endpoints are created
        mockServer.getClient().endpoints().inNamespace("test").resource(endpoint).create();
        mockServer.getClient().pods().inNamespace("test").resource(pod).create();
    }

    private void pauseScheduler() {
        scheduler.pause();
        await().atMost(30, SECONDS).until(() -> !scheduler.isRunning());
    }

    private void createPostgresqlService() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/json")
                .body("{\"name\": \"PostgreSQL\", \"version\": \"8\", \"endpoint\": \"tcp:5432\", \"deployed\": \"true\" }")
                .when().post("/services")
                .then()
                .statusCode(201)
                .extract()
                .as(Service.class);
    }

    private void createRabitMQService() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/json")
                .body("{\"name\": \"RabbitMQ\", \"version\": \"3.11.2\", \"endpoint\": \"tcp:5672\", \"deployed\": \"false\" }")
                .when().post("/services")
                .then()
                .statusCode(201);
    }
}
