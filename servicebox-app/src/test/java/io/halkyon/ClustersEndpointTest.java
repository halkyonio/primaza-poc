package io.halkyon;

import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;

import io.halkyon.model.Cluster;
import io.halkyon.services.KubernetesClientService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
public class ClustersEndpointTest {

    @InjectMock
    KubernetesClientService mockKubernetesClientService;

    @Test
    public void testAddClusterViaHtmxForm(){
        // A htmx request will contain a HX-Request header and Content-Type: application/x-www-form-urlencoded
        given()
                .header("HX-Request", true)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .multiPart("name", "ocp4.11-node-2")
                .multiPart("environment", "TEST")
                .multiPart("namespaces","kube-system,ingress")
                .multiPart("url", "https://10.0.2.12:6443")
                .when().post("/clusters")
                .then()
                .statusCode(201);
    }


}
