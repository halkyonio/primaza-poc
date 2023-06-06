package io.halkyon.services;

import static io.halkyon.utils.StringUtils.getHostFromUrl;
import static io.halkyon.utils.StringUtils.getPortFromUrl;
import static io.halkyon.utils.StringUtils.toBase64;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.halkyon.exceptions.ClusterConnectException;
import io.halkyon.model.*;
import io.halkyon.utils.StringUtils;
import io.quarkus.vault.VaultKVSecretEngine;

@ApplicationScoped
public class BindApplicationService {

    private static final Logger LOG = Logger.getLogger(BindApplicationService.class);

    public static final String TYPE_KEY = "type";
    public static final String URL_KEY = "url";
    public static final String HOST_KEY = "host";
    public static final String PORT_KEY = "port";
    public static final String USERNAME_KEY = "username";
    public static final String PASSWORD_KEY = "password";

    public static final String DATABASE_KEY = "database";

    @Inject
    VaultKVSecretEngine kvSecretEngine;

    @Inject
    KubernetesClientService kubernetesClientService;

    public void unBindApplication(Claim claim) throws ClusterConnectException {
        kubernetesClientService.unMountSecretVolumeEnvInApplication(claim.application);
        String secretName = KubernetesClientService.getSecretName(claim.application);
        kubernetesClientService.deleteApplicationSecret(secretName, claim.application.cluster,
                claim.application.namespace);
        kubernetesClientService.rolloutApplication(claim.application);
        // TODO: Test should be improved to test if the service has been deployed using Crossplane
        if (claim.service.installable) {
            kubernetesClientService.deleteRelease(claim);
        }
    }

    public void bindApplication(Claim claim) throws ClusterConnectException {
        Optional<Credential> optCredential = getFirstCredentialFromService(claim.service);
        if (optCredential.isPresent()) {
            Credential credential = optCredential.get();
            String url = generateUrlByClaimService(claim);
            claim.credential = credential;
            claim.url = url;
            claim.status = ClaimStatus.BOUND.toString();
            claim.persist();
            if (url != null) {
                // scenario is supported
                Map<String, String> secret = createSecret(claim.type, credential, url);
                kubernetesClientService.mountSecretInApplication(claim.application, secret);
                kubernetesClientService.rolloutApplication(claim.application);
            }
        } else {
            LOG.errorf("Credential not found for service %s. Impossible binding", claim.service.name);
            claim.status = ClaimStatus.ERROR.toString();
            claim.persist();
        }
    }

    private Map<String, String> createSecret(String type, Credential credential, String url) {
        Map<String, String> secretData = new HashMap<>();
        secretData.put(TYPE_KEY, toBase64(type));
        secretData.put(HOST_KEY, toBase64(getHostFromUrl(url)));
        secretData.put(PORT_KEY, toBase64(getPortFromUrl(url)));
        secretData.put(URL_KEY, toBase64(url));

        String username = "";
        String password = "";
        String database = "";

        if (StringUtils.isNotEmpty(credential.username) && StringUtils.isNotEmpty(credential.password)) {
            username = credential.username;
            password = credential.password;
            for (CredentialParameter param : credential.params) {
                secretData.put(param.paramName, toBase64(param.paramValue));
            }
        }

        if (StringUtils.isNotEmpty(credential.vaultKvPath)) {
            Map<String, String> vaultSecret = kvSecretEngine.readSecret(credential.vaultKvPath);
            Set<String> vaultSet = vaultSecret.keySet();
            for (String key : vaultSet) {
                if (key.equals(USERNAME_KEY)) {
                    username = vaultSecret.get(USERNAME_KEY);
                    credential.username = username;
                } else if (key.equals(PASSWORD_KEY)) {
                    password = vaultSecret.get(PASSWORD_KEY);
                    credential.password = password;
                } else if (key.equals(DATABASE_KEY)) {
                    database = vaultSecret.get(DATABASE_KEY);
                } else {
                    secretData.put(key, vaultSecret.get(key));
                    CredentialParameter credentialParameter = new CredentialParameter();
                    credentialParameter.paramName = key;
                    credentialParameter.paramValue = vaultSecret.get(key);
                    credential.params.add(credentialParameter);
                }
            }
        }
        secretData.put(USERNAME_KEY, toBase64(username));
        secretData.put(PASSWORD_KEY, toBase64(password));
        if (StringUtils.isNotEmpty(database)) {
            secretData.put(DATABASE_KEY, toBase64(database));
        }

        return secretData;
    }

    private Optional<Credential> getFirstCredentialFromService(Service service) {
        if (service.credentials != null) {
            return service.credentials.stream().findFirst();
        }
        return Optional.empty();

    }

    private String generateUrlByClaimService(Claim claim) {
        Application application = claim.application;
        Service service = claim.service;
        LOG.debugf("Application cluster name: %s", application.cluster.name == null ? "" : application.cluster.name);
        LOG.debugf("Application namespace: %s", application.name == null ? "" : application.namespace);

        LOG.debugf("Service cluster: %s", service.cluster == null ? "" : service.cluster);
        LOG.debugf("Service name: %s", service.name == null ? "" : service.name);
        LOG.debugf("Service namespace: %s", service.namespace == null ? "" : service.namespace);
        LOG.debugf("Service port: %s", service.getPort() == null ? "" : service.getPort());
        LOG.debugf("Service protocol: %s", service.getProtocol() == null ? "" : service.getProtocol());

        if (Objects.equals(application.cluster, service.cluster)
                && Objects.equals(application.namespace, service.namespace)) {
            LOG.debugf("Rule 1: app + service within same ns, cluster");
            // rule 1: app + service within same ns, cluster
            // -> app can access the service using: protocol://service_name:port
            return String.format("%s://%s:%s", service.getProtocol(), service.name, service.getPort());
        } else if (Objects.equals(application.cluster, service.cluster)) {
            LOG.debugf("Rule 2: app + service in different ns, same cluster");
            // rule 2: app + service in different ns, same cluster
            // -> app can access the service using: protocol://service_name.namespace:port
            return String.format("%s://%s.%s:%s", service.getProtocol(), service.name, service.namespace,
                    service.getPort());
        } else if (StringUtils.isNotEmpty(service.externalEndpoint)) {
            LOG.debugf("Rule 3 and 4: app + service running in another cluster using external IP");
            // rule 3 and 4: app + service running in another cluster using external IP
            // -> app can access the service using: protocol://service-external-ip:port
            return String.format("%s://%s:%s", service.getProtocol(), service.externalEndpoint, service.getPort());
        }

        return null;
    }
}
