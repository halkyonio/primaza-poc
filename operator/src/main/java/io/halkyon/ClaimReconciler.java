package io.halkyon;

import static io.halkyon.utils.StringUtils.getHostFromUrl;
import static io.halkyon.utils.StringUtils.getPortFromUrl;
import static io.halkyon.utils.StringUtils.isNotEmpty;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

@ControllerConfiguration(namespaces = "app", name = "claimator")
public class ClaimReconciler implements Reconciler<Claim> {
    private final KubernetesClient client;

    private static final String SERVICE_BINDING_ROOT = "SERVICE_BINDING_ROOT";
    private static final String SERVICE_BINDING_ROOT_DEFAULT_VALUE = "/bindings";

    public static final String TYPE_KEY = "type";
    public static final String URL_KEY = "url";
    public static final String HOST_KEY = "host";
    public static final String PORT_KEY = "port";
    public static final String USERNAME_KEY = "username";
    public static final String PASSWORD_KEY = "password";

    public static final String DATABASE_KEY = "database";

    public ClaimReconciler(KubernetesClient client) {
        this.client = client;
    }

    // TODO Fill in the rest of the reconciler

    @Override
    public UpdateControl<Claim> reconcile(Claim resource, Context context) {

        Map<String, String> secretData = createSecret("type", resource.getSpec().credential, resource.getSpec().url);

        mountSecretInApplication(resource, secretData);
        client.apps().deployments().inNamespace(resource.getMetadata().getNamespace())
                .withName(resource.getSpec().application).rolling().restart();

        return UpdateControl.noUpdate();
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

        if (isNotEmpty(credential.username) && isNotEmpty(credential.password)) {
            username = credential.username;
            password = credential.password;
            // for (CredentialParameter param : credential.params) {
            // secretData.put(param.paramName, toBase64(param.paramValue));
            // }
        }

        // if (StringUtils.isNotEmpty(credential.vaultKvPath)) {
        // Map<String, String> vaultSecret = kvSecretEngine.readSecret(credential.vaultKvPath);
        // Set<String> vaultSet = vaultSecret.keySet();
        // for (String key : vaultSet) {
        // if (key.equals(USERNAME_KEY)) {
        // username = vaultSecret.get(USERNAME_KEY);
        // credential.username = username;
        // } else if (key.equals(PASSWORD_KEY)) {
        // password = vaultSecret.get(PASSWORD_KEY);
        // credential.password = password;
        // } else if (key.equals(DATABASE_KEY)) {
        // database = vaultSecret.get(DATABASE_KEY);
        // } else {
        // secretData.put(key, vaultSecret.get(key));
        // CredentialParameter credentialParameter = new CredentialParameter();
        // credentialParameter.paramName = key;
        // credentialParameter.paramValue = vaultSecret.get(key);
        // credential.params.add(credentialParameter);
        // }
        // }
        // }
        secretData.put(USERNAME_KEY, toBase64(username));
        secretData.put(PASSWORD_KEY, toBase64(password));
        if (isNotEmpty(database)) {
            secretData.put(DATABASE_KEY, toBase64(database));
        }
        return secretData;
    }

    /**
     * Add the secret into the specified cluster and namespace.
     */
    public void mountSecretInApplication(Claim claim, Map<String, String> secretData) {
        // Application application = claim.application;
        // client = getClientForCluster(application.cluster);

        // create secret
        ClaimSpec claimSpec = claim.getSpec();
        String secretName = (claimSpec.application + "-secret").toLowerCase(Locale.ROOT);
        String namespace = claim.getMetadata().getNamespace();
        client.secrets().inNamespace(namespace).resource(new SecretBuilder().withNewMetadata().withName(secretName)
                .withNamespace(namespace).endMetadata().withData(secretData).build()).create();

        /*
         * Get the Deployment resource to be updated
         */
        Deployment deployment = client.apps().deployments().inNamespace(namespace).withName(claimSpec.application)
                .get();

        /*
         * Add a volumeMount to the container able to mount the path to access the secret under
         * "/SERVICE_BINDING_ROOT/secretName"
         *
         * Pass as ENV the property "SERVICE_BINDING_ROOT" pointing to the mount dir (e.g /bindings)
         *
         * Mount the secret
         */
        Deployment newDeployment = new DeploymentBuilder(deployment).accept(ContainerBuilder.class, container -> {
            container.removeMatchingFromVolumeMounts(vm -> Objects.equals(secretName, vm.getName())
                    && Objects.equals(SERVICE_BINDING_ROOT_DEFAULT_VALUE + "/" + secretName, vm.getMountPath()));
            container.addNewVolumeMount().withName(secretName)
                    .withMountPath(SERVICE_BINDING_ROOT_DEFAULT_VALUE + "/" + secretName).endVolumeMount();
            container.removeMatchingFromEnv(e -> Objects.equals(SERVICE_BINDING_ROOT, e.getName()));
            container.addNewEnv().withName(SERVICE_BINDING_ROOT).withValue(SERVICE_BINDING_ROOT_DEFAULT_VALUE).endEnv();
        }).accept(PodSpecBuilder.class, podSpec -> {
            podSpec.removeMatchingFromVolumes(v -> Objects.equals(secretName, v.getName()));
            podSpec.addNewVolume().withName(secretName).withNewSecret().withSecretName(secretName).endSecret()
                    .endVolume();
        }).build();

        // logIfDebugEnabled(newDeployment);
        try {
            // update deployment
            client.apps().deployments().inNamespace(namespace).resource(newDeployment).patch();
        } catch (Exception e) {
            client.secrets().inNamespace(namespace).withName(secretName).delete();
        }
    }

    public static String toBase64(String paramValue) {
        return Base64.getEncoder().encodeToString(paramValue.getBytes(StandardCharsets.UTF_8));
    }
}
