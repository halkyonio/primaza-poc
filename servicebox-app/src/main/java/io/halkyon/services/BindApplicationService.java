package io.halkyon.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.halkyon.model.Application;
import io.halkyon.model.Claim;
import io.halkyon.model.Credential;
import io.halkyon.model.CredentialParameter;
import io.halkyon.model.Service;

import static io.halkyon.utils.StringUtils.getHostFromUrl;
import static io.halkyon.utils.StringUtils.getPortFromUrl;
import static io.halkyon.utils.StringUtils.removeSchemeFromUrl;
import static io.halkyon.utils.StringUtils.toBase64;

@ApplicationScoped
public class BindApplicationService {

    public static final String TYPE_KEY = "type";
    public static final String URI_KEY = "uri";
    public static final String URL_KEY = "url";
    public static final String HOST_KEY = "host";
    public static final String PORT_KEY = "port";
    public static final String USERNAME_KEY = "username";
    public static final String PASSWORD_KEY = "password";

    @Inject
    KubernetesClientService kubernetesClientService;

    public void unBindApplication(Application application, Claim claim) {
        unMountSecretVolumeEnvInApplication(application, claim);
        deleteSecretInNamespace(application, claim);
        rolloutApplication(application);
    }

    public void bindApplication(Application application, Claim claim) {
        Credential credential = getFirstCredentialFromService(claim.service);
        String url = generateUrlByClaimService(application, claim);
        claim.credential = credential;
        claim.url = url;
        claim.persist();
        if (credential != null && url != null) {
            // scenario is supported
            createSecretForApplication(application, claim, credential, url);
            rolloutApplication(application);
        }
    }

    private void rolloutApplication(Application application) {
        kubernetesClientService.rolloutApplication(application);
    }

    private void deleteSecretInNamespace(Application application, Claim claim) {
        kubernetesClientService.deleteSecretInNamespace(application, claim);
    }

    private void unMountSecretVolumeEnvInApplication(Application application, Claim claim) {
        kubernetesClientService.unMountSecretVolumeEnvInApplication(application, claim);
    }

    private void createSecretForApplication(Application application, Claim claim, Credential credential, String url) {
        Map<String, String> secretData = new HashMap<>();
        secretData.put(TYPE_KEY, toBase64(claim.type));
        secretData.put(URI_KEY, toBase64(removeSchemeFromUrl(url)));
        secretData.put(HOST_KEY, toBase64(getHostFromUrl(url)));
        secretData.put(PORT_KEY, toBase64(getPortFromUrl(url)));
        secretData.put(URL_KEY, toBase64(url));
        secretData.put(USERNAME_KEY, toBase64(credential.username));
        secretData.put(PASSWORD_KEY, toBase64(credential.password));

        for (CredentialParameter param : credential.params) {
            secretData.put(param.paramName, toBase64(param.paramValue));
        }

        kubernetesClientService.mountSecretInApplication(application, claim, secretData);
    }

    private Credential getFirstCredentialFromService(Service service) {
        if (service.credentials == null || service.credentials.isEmpty()) {
            return null;
        }

        return service.credentials.get(0);
    }

    private String generateUrlByClaimService(Application application, Claim claim) {
        Service service = claim.service;
        if (application.cluster.id == service.cluster.id
                && Objects.equals(application.namespace, service.namespace)) {
            // rule 1: app + service within same ns, cluster
            //         -> app can access the service using: protocol://service_name:port
            return String.format("%s://%s:%s", service.getProtocol(), service.name, service.getPort());
        } else if (application.cluster.id == service.cluster.id) {
            // rule 2: app + service in different ns, same cluster
            //         -> app can access the service using: protocol://service_name.namespace:port
            return String.format("%s://%s.%s:%s", service.getProtocol(), service.name, service.namespace, service.getPort());
        }
        // TODO: rule 3: app + service running in another cluster. https://github.com/halkyonio/primaza-poc/issues/134
        // TODO: rule 4: app + service running on a standalone machine. https://github.com/halkyonio/primaza-poc/discussions/135

        return null;
    }
}
