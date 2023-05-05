## How to use Crossplane & Helm provider

- Install first the `kubectl crossplane` client (optional):
```bash
curl -sL https://raw.githubusercontent.com/crossplane/crossplane/master/install.sh | sh
mv kubectl-crossplane /usr/local/bin
```
- Add the crossplane helm repo
```bash
helm repo add crossplane-stable https://charts.crossplane.io/stable && helm repo update
```
- Deploy the crossplane chart
```bash
helm uninstall crossplane -n crossplane-system
helm install crossplane \
  -n crossplane-system \
  --create-namespace \
  crossplane-stable/crossplane
```

- Install the Helm provider
```bash
cat <<EOF | kubectl apply -f -
apiVersion: pkg.crossplane.io/v1
kind: Provider
metadata:
  name: helm-provider
spec:
  package: "crossplanecontrib/provider-helm:master"
EOF
```
- Give more RBAC rights to the crossplane SA
```bash
SA=$(kubectl -n crossplane-system get sa -o name | grep helm-provider | sed -e 's|serviceaccount\/|crossplane-system:|g')
kubectl create clusterrolebinding helm-provider-admin-binding --clusterrole cluster-admin --serviceaccount="${SA}"
```
- Create the ProviderConfig
```bash
cat <<EOF | kubectl apply -f -
apiVersion: helm.crossplane.io/v1beta1
kind: ProviderConfig
metadata:
  name: helm-provider
spec:
  credentials:
    source: InjectedIdentity
EOF
```
- To test crossplane and the helm provider, install the following helm chart using a `Release` CR
```bash
cat <<EOF | kubectl apply -f -
apiVersion: helm.crossplane.io/v1beta1
kind: Release
metadata:
  name: postgresql
spec:
  forProvider:
    chart:
      name: postgresql
      repository: https://charts.bitnami.com/bitnami
      version: 11.9.1
    namespace: db
    skipCreateNamespace: false
    wait: true
    set:
    - name: auth.username
      value: healthy
    - name: auth.password
      value: healthy
    - name: auth.database
      value: fruits_database
  providerConfigRef:
    name: helm-provider
EOF
```
>**Note**: You can deploy the release file using the command `kubectl apply -f ./scripts/data/release-postgresql.yml`

## Deploy a Helm DB chart using Composite and Compose resources

Instead of deploying a Helm Release to request directly to the Crossplane Helm provider to deploy a Helm chart, we will now use
a `Database` composite resource (aka our own CRD) and a `Composition` resource containing the template and patches to generate the needed resources: `Release`, etc

Deploy first the Database CRD and composition resource
```bash
kubectl apply -f ./crossplane/database-helm/composite.yml
kubectl apply -f ./crossplane/database-helm/composition.yml
```

To install by example a postgresql helm chart under the namespace `db` using the version `11.9.1`, creat and deploy the following resource:
```bash
cat <<EOF | kubectl apply -f -
apiVersion: snowdrop.dev/v1alpha1
kind: Database
metadata:
  name: postgresql-db
spec:
  compositionSelector:
    matchLabels:
      provider: local
      type: dev
  parameters:
    type: postgresql
    version: 11.9.1
    namespace: db
EOF
```
Check the `database` status
```bash
kubectl describe database/postgresql-db
Name:         postgresql-db
Namespace:    
Labels:       crossplane.io/composite=postgresql-db
Annotations:  <none>
API Version:  snowdrop.dev/v1alpha1
Kind:         Database
...
Spec:
  Parameters:
    Namespace:  db
    Type:       postgresql
    Version:    11.9.1
...
Events:
  Type    Reason                   Age                From                                                             Message
  ----    ------                   ----               ----                                                             -------
  Normal  CompositionUpdatePolicy  15s                defined/compositeresourcedefinition.apiextensions.crossplane.io  Default composition update policy has been selected
  Normal  PublishConnectionSecret  15s                defined/compositeresourcedefinition.apiextensions.crossplane.io  Successfully published connection details
  Normal  ComposeResources         15s (x2 over 15s)  defined/compositeresourcedefinition.apiextensions.crossplane.io  Composed resource "postgresql-helm-release" is not yet ready
  Normal  SelectComposition        14s (x4 over 15s)  defined/compositeresourcedefinition.apiextensions.crossplane.io  Successfully selected composition
  Normal  ComposeResources         14s (x4 over 15s)  defined/compositeresourcedefinition.apiextensions.crossplane.io  Successfully composed resources
```

A podtgresql pod should be created soon:
```bash
kubectl get pod -lapp.kubernetes.io/name=postgresql -n db
NAME              READY   STATUS    RESTARTS   AGE
postgresql-db-0   1/1     Running   0          2m41s
```
To clean up:

```bash
kubectl delete -f ./crossplane/database-helm
```


## How to use Upbound

Documentation page: https://docs.upbound.io/uxp/install/

- Install first the clients: `Up`
```bash
curl -sL "https://cli.upbound.io" | sh
mv up /usr/local/bin
```
- Deploy up on the kind cluster
```bash
up uxp install
```
