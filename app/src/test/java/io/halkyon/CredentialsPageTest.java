package io.halkyon;

import static io.halkyon.utils.TestUtils.createCredential;
import static io.halkyon.utils.TestUtils.createService;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.halkyon.model.Credential;
import io.halkyon.model.Service;
import io.halkyon.services.KubernetesClientService;
import io.halkyon.utils.WebPageExtension;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
@QuarkusTestResource(WebPageExtension.class)
public class CredentialsPageTest {

    @InjectMock
    KubernetesClientService mockKubernetesClientService;

    WebPageExtension.PageManager page;

    @Test
    public void testCreateNewCredential() {
        createService("postgresql-credential1", "8", "postgresql");
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
        page.select("credential_service", "postgresql-credential1-8");
        page.type("credential_name", "Credential1");
        page.type("credential_username", "Admin");
        page.type("credential_password", "Supersecret");
        page.type("credential_vault_path", "myapps/vault-quickstart/private");
        // submit credential
        page.clickById("credential-button");

        // then, the new credential should be listed:
        page.goTo("/credentials");
        page.assertContentContains("Credential1");
        Credential credential = given().when().get("/credentials/name/Credential1").then().statusCode(200).extract()
                .as(Credential.class);
        assertEquals("Admin", credential.username);
        assertEquals("Supersecret", credential.password);
        assertEquals("myapps/vault-quickstart/private", credential.vaultKvPath);
        assertEquals(2, credential.params.size());
        assertEquals("a", credential.params.get(0).paramName);
        assertEquals("1", credential.params.get(0).paramValue);
        assertEquals("b", credential.params.get(1).paramName);
        assertEquals("2", credential.params.get(1).paramValue);

        // and the service should have been linked to it.
        Service service = given().when().get("/services/name/postgresql-credential1").then().statusCode(200).extract()
                .as(Service.class);
        assertEquals(1, service.credentials.size());
        assertEquals("Credential1", service.credentials.get(0).name);
    }

    @Test
    public void testEditCredentialFromPage() {
        // Create data
        String prefix = "CredentialsPageTest-testEditCredentialFromPage-";
        Service service = createService(prefix + "service", "8", "type");
        Credential credential = createCredential(prefix + "credential", service.id, "user", "pass",
                "myapps/vault-quickstart/private");
        // Go to the page
        page.goTo("/credentials");
        // Ensure our data is listed
        page.assertContentContains(credential.name);
        // Let's change the owner
        page.clickById("btn-credential-edit-" + credential.id);
        page.assertPathIs("/credentials/" + credential.id);
        page.assertContentContains("Update Credential");
        page.assertContentContains(credential.name);
        page.type("credential_name", credential.name + "new");
        page.clickById("credential-button");
        // Verify the entity was properly updated:
        page.assertContentContains("Updated successfully for id: " + credential.id);
        // Go back to the credentials list and check whether the owner is displayed
        page.goTo("/credentials");
        page.assertContentContains(credential.name + "new");
    }

    @Test
    public void testDeleteCredential() {
        String prefix = "CredentialsPageTest-testDeleteCredential-";
        Service service = createService(prefix + "service", "8", "postgresql");
        Credential credential = createCredential(prefix + "credential", service.id, "user", "pass",
                "myapps/vault-quickstart/private");

        // When, we go to the credentials page
        page.goTo("/credentials");
        page.assertContentContains(credential.name);

        // And click on delete
        page.clickById("btn-credential-delete-" + credential.id);

        // Then, the credential should have been deleted
        page.goTo("/credentials");
        page.assertContentDoesNotContain(credential.name);
    }

}
