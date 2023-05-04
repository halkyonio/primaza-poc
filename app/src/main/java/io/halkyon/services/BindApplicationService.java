package io.halkyon.services;

import static io.halkyon.utils.StringUtils.getHostFromUrl;
import static io.halkyon.utils.StringUtils.getPortFromUrl;
import static io.halkyon.utils.StringUtils.toBase64;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.halkyon.exceptions.ClusterConnectException;
import io.halkyon.model.*;
import io.halkyon.utils.StringUtils;
import io.quarkus.vault.VaultKVSecretEngine;

@ApplicationScoped
public class BindApplicationService {

    public static final String TYPE_KEY = "type";
    public static final String URL_KEY = "url";
    public static final String HOST_KEY = "host";
    public static final String PORT_KEY = "port";
    public static final String USERNAME_KEY = "username";
    public static final String PASSWORD_KEY = "password";

    public static final String VAULT_KV_PATH_KEY = "vault-path";

    @Inject
    VaultKVSecretEngine kvSecretEngine;

    @Inject
    KubernetesClientService kubernetesClientService;

    public void unBindApplication(Claim claim) throws ClusterConnectException {
        unMountSecretVolumeEnvInApplication(claim);
        deleteSecretInNamespace(claim);
        removeIngressHostFromApplication(claim);
        rolloutApplication(claim);
    }

    private void removeIngressHostFromApplication(Claim claim) {
        Application app = claim.application;
        app.ingress = "";
        app.persist();
    }

    public void bindApplication(Claim claim) throws ClusterConnectException {
        Credential credential = getFirstCredentialFromService(claim.service);
        String url = generateUrlByClaimService(claim);
        claim.credential = credential;
        claim.url = url;
        claim.status = ClaimStatus.BOUND.toString();
        claim.persist();
        if (credential != null && url != null) {
            // scenario is supported
            createSecretForApplication(claim, credential, url);
            rolloutApplication(claim);
            if (claim.status.equals(ClaimStatus.BOUND.toString())) {
                Application app = claim.application;
                app.ingress = getIngressHost(app);
                app.persist();
            }
        }
    }

    private void rolloutApplication(Claim claim) throws ClusterConnectException {
        kubernetesClientService.rolloutApplication(claim.application);
    }

    private String getIngressHost(Application application) throws ClusterConnectException {
        return kubernetesClientService.getIngressHost(application);
    }

    private void deleteSecretInNamespace(Claim claim) throws ClusterConnectException {
        kubernetesClientService.deleteSecretInNamespace(claim);
    }

    private void unMountSecretVolumeEnvInApplication(Claim claim) throws ClusterConnectException {
        kubernetesClientService.unMountSecretVolumeEnvInApplication(claim);
    }

    public void createCrossplaneHelmRelease() throws ClusterConnectException {
        // TODO: How could we get the Cluster object which is needed here to get the Kubernetes Client !!
        kubernetesClientService.createCrossplaneHelmRelease();
    }

    private void createSecretForApplication(Claim claim, Credential credential, String url)
            throws ClusterConnectException {
        String username = credential.username;
        String password = credential.password;

        if (StringUtils.isNotEmpty(credential.vaultKvPath)) {
            Map<String, String> vaultSecret = kvSecretEngine.readSecret(credential.vaultKvPath);
            Set<String> usernames = vaultSecret.keySet();
            username = usernames.iterator().next();
            password = vaultSecret.get(username);
        }

        Map<String, String> secretData = new HashMap<>();
        secretData.put(TYPE_KEY, toBase64(claim.type));
        secretData.put(HOST_KEY, toBase64(getHostFromUrl(url)));
        secretData.put(PORT_KEY, toBase64(getPortFromUrl(url)));
        secretData.put(URL_KEY, toBase64(url));
        secretData.put(USERNAME_KEY, toBase64(username));
        secretData.put(PASSWORD_KEY, toBase64(password));

        for (CredentialParameter param : credential.params) {
            secretData.put(param.paramName, toBase64(param.paramValue));
        }

        kubernetesClientService.mountSecretInApplication(claim, secretData);
    }

    private Credential getFirstCredentialFromService(Service service) {
        if (service.credentials == null || service.credentials.isEmpty()) {
            return null;
        }

        return service.credentials.get(0);
    }

    private String generateUrlByClaimService(Claim claim) {
        Application application = claim.application;
        Service service = claim.service;
        if (Objects.equals(application.cluster, service.cluster)
                && Objects.equals(application.namespace, service.namespace)) {
            // rule 1: app + service within same ns, cluster
            // -> app can access the service using: protocol://service_name:port
            return String.format("%s://%s:%s", service.getProtocol(), service.name, service.getPort());
        } else if (Objects.equals(application.cluster, service.cluster)) {
            // rule 2: app + service in different ns, same cluster
            // -> app can access the service using: protocol://service_name.namespace:port
            return String.format("%s://%s.%s:%s", service.getProtocol(), service.name, service.namespace,
                    service.getPort());
        } else if (StringUtils.isNotEmpty(service.externalEndpoint)) {
            // rule 3 and 4: app + service running in another cluster using external IP
            // -> app can access the service using: protocol://service-external-ip:port
            return String.format("%s://%s:%s", service.getProtocol(), service.externalEndpoint, service.getPort());
        }

        return null;
    }
}
