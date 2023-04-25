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
  name: provider-helm
spec:
  package: "crossplanecontrib/provider-helm:master"
EOF
```
- Give more RBAC rights to the crossplane SA
```bash
SA=$(kubectl -n crossplane-system get sa -o name | grep provider-helm | sed -e 's|serviceaccount\/|crossplane-system:|g')
kubectl create clusterrolebinding provider-helm-admin-binding --clusterrole cluster-admin --serviceaccount="${SA}"
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
cat <<EOF | kubectl apply -f 
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
