package io.halkyon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.halkyon.model.Credential;
import io.halkyon.model.CredentialParameter;
import io.halkyon.model.Service;
import io.halkyon.resource.requests.CredentialRequest;
import io.halkyon.services.CredentialService;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CredentialServiceTest extends BaseTest {

    @Inject
    CredentialService credentialService;

    @InjectMock
    Session session;

    @Test
    public void shouldPersistCredential() {

        Credential mock = mock(Credential.class);
        when(mock.id).thenReturn(null);

        credentialService.doSave(mock);

        verify(mock, times(1)).persist();

    }

    @Test
    public void shouldPersistEditedCredential() {

        // Prepare
        PanacheMock.mock(Credential.class);

        Credential credential = new Credential();
        credential.id = 1L;
        credential.name = "TestCredential";
        credential.service = mock(Service.class);
        credential.username = "testuser";
        credential.password = "testpassword";
        credential.vaultKvPath = "some/path";
        CredentialParameter cp = new CredentialParameter();
        cp.paramName = "foo";
        cp.paramValue = "bar";
        credential.params.add(cp);

        Credential edited = new Credential();
        edited.id = 1L;
        edited.name = "TestEditedCredential";
        edited.service = mock(Service.class);
        edited.username = "testEditeduser";
        edited.password = "testEditedpassword";
        edited.vaultKvPath = "another/path";
        CredentialParameter editedParams = new CredentialParameter();
        editedParams.paramName = "foz";
        editedParams.paramValue = "baz";
        edited.params = Arrays.asList(editedParams);

        when(Credential.findById(1L)).thenReturn(credential);

        credentialService.doSave(edited);

        // PanacheMock.verify(Credential.class, times(1)).findById(1L);
        Mockito.verify(session, Mockito.times(1)).persist(credential);

        assertEquals("TestEditedCredential", credential.name);
        assertEquals("testEditeduser", credential.username);
        assertEquals("testEditedpassword", credential.password);
        assertEquals(credential.service, edited.service);
        assertEquals("another/path", credential.vaultKvPath);
        assertEquals(1, credential.params.size());
        assertEquals("foz", credential.params.get(0).paramName);
        assertEquals("baz", credential.params.get(0).paramValue);

    }

    @Test
    public void shouldCreateUsernameAndPasswordCredentialFromRequest() {

        // Prepare
        PanacheMock.mock(Service.class);
        Service mock = mock(Service.class);
        when(Service.findById(1L)).thenReturn(mock);

        // Given a CredentialRequest
        CredentialRequest request = new CredentialRequest();
        request.name = "TestCredential";
        request.username = "testuser";
        request.password = "testpassword";
        request.serviceId = 1L;

        // When Call the initializeCredential method
        Credential credential = credentialService.initializeCredential(request);

        // Then Verify that the method returns the expected Credential
        assertEquals("TestCredential", credential.name);
        assertEquals("testuser", credential.username);
        assertEquals("testpassword", credential.password);
        assertEquals(mock, credential.service);
        assertEquals(null, credential.vaultKvPath);
    }

    @Test
    public void shouldCreateVaultKvPathCredentialFromRequest() {

        // Prepare
        PanacheMock.mock(Service.class);
        Service mock = mock(Service.class);
        when(Service.findById(1L)).thenReturn(mock);

        // Given a CredentialRequest
        CredentialRequest request = new CredentialRequest();
        request.name = "TestCredential";
        request.vaultKvPath = "some/path";
        request.serviceId = 1L;

        // When Call the initializeCredential method
        Credential credential = credentialService.initializeCredential(request);

        // Then Verify that the method returns the expected Credential
        assertEquals("TestCredential", credential.name);
        assertEquals(null, credential.username);
        assertEquals(null, credential.password);
        assertEquals(mock, credential.service);
        assertEquals("some/path", credential.vaultKvPath);
    }

    @Test
    public void shouldCreateVaultKvPathCredentialFromRequestWhenPathStartsWithSlash() {

        // Prepare
        PanacheMock.mock(Service.class);
        Service mock = mock(Service.class);
        when(Service.findById(1L)).thenReturn(mock);

        // Given a CredentialRequest
        CredentialRequest request = new CredentialRequest();
        request.name = "TestCredential";
        request.vaultKvPath = "/some/path";
        request.serviceId = 1L;

        // When Call the initializeCredential method
        Credential credential = credentialService.initializeCredential(request);

        // Then Verify that the method returns the expected Credential
        assertEquals("TestCredential", credential.name);
        assertEquals(null, credential.username);
        assertEquals(null, credential.password);
        assertEquals(mock, credential.service);
        assertEquals("some/path", credential.vaultKvPath);
    }

    @Test
    public void shouldCreateCredentialWithParams() {

        // Prepare
        PanacheMock.mock(Service.class);
        Service mock = mock(Service.class);
        when(Service.findById(1L)).thenReturn(mock);

        // Given a CredentialRequest
        CredentialRequest request = new CredentialRequest();
        request.name = "TestCredential";
        request.serviceId = 1L;
        List<String> params = Arrays.asList("foo:bar");
        request.params = params;

        // When Call the initializeCredential method
        Credential credential = credentialService.initializeCredential(request);

        // Then Verify that the method returns the expected Credential
        assertEquals("TestCredential", credential.name);
        assertEquals(1, credential.params.size());
        assertEquals("foo", credential.params.get(0).paramName);
        assertEquals("bar", credential.params.get(0).paramValue);
    }

}
