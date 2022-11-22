package io.halkyon;

import static io.halkyon.utils.TestUtils.createService;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.halkyon.model.Credential;
import io.halkyon.model.Service;
import io.halkyon.utils.WebPageExtension;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(WebPageExtension.class)
public class CredentialsPageTest {

    WebPageExtension.PageManager page;

    @Test
    public void testCreateNewCredential(){
        createService("postgresql-credential1", "8", true);
        page.goTo("/credentials/new");
        // add param a=1
        page.type("new-param-name", "a");
        page.type("new-param-value", "1");
        page.clickById("add-param-to-credential-button");
        // add param b=2
        page.type("new-param-name", "b");
        page.type("new-param-value", "2");
        page.clickById("add-param-to-credential-button");
        // set data
        page.select("credential_service", "postgresql-credential1");
        page.type("credential_name", "Credential1");
        page.type("credential_username", "Admin");
        page.type("credential_password", "Supersecret");
        // submit credential
        page.clickById("new-credential-button");

        // then, the new credential should be listed:
        page.goTo("/credentials");
        page.assertContentContains("Credential1");
        Credential credential = given()
                .when().get("/credentials/name/Credential1")
                .then()
                .statusCode(200)
                .extract().as(Credential.class);
        assertEquals("Admin", credential.username);
        assertEquals("Supersecret", credential.password);
        assertEquals(2, credential.params.size());
        assertEquals("a", credential.params.get(0).paramName);
        assertEquals("1", credential.params.get(0).paramValue);
        assertEquals("b", credential.params.get(1).paramName);
        assertEquals("2", credential.params.get(1).paramValue);

        // and the service should have been linked to it.
        Service service = given()
                .when().get("/services/name/postgresql-credential1")
                .then()
                .statusCode(200)
                .extract().as(Service.class);
        assertEquals(1, service.credentials.size());
        assertEquals("Credential1", service.credentials.get(0).name);
    }

}
