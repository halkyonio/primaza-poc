## How to use Crossplane & Helm provider

- Install first the clients: `kubectl crossplane`:
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
kubectl crossplane install provider crossplanecontrib/provider-helm:master
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
- Install a helm chart using a release
```bash
cat <<EOF | kubectl apply -f -
apiVersion: helm.crossplane.io/v1beta1
kind: Release
metadata:
  name: wordpress-example
spec:
# rollbackLimit: 3
  forProvider:
    chart:
      name: wordpress
      repository: https://charts.bitnami.com/bitnami
      version: 15.2.5 ## To use development versions, set ">0.0.0-0"
#     pullSecretRef:
#       name: museum-creds
#       namespace: default
#     url: "https://charts.bitnami.com/bitnami/wordpress-9.3.19.tgz"
    namespace: wordpress
#   insecureSkipTLSVerify: true
#   skipCreateNamespace: true
#   wait: true
#   skipCRDs: true
    values:
      service:
        type: ClusterIP
    set:
      - name: param1
        value: value2
#   valuesFrom:
#     - configMapKeyRef:
#         key: values.yaml
#         name: default-vals
#         namespace: wordpress
#         optional: false
#     - secretKeyRef:
#         key: svalues.yaml
#         name: svals
#         namespace: wordpress
#         optional: false
#  connectionDetails:
#    - apiVersion: v1
#      kind: Service
#      name: wordpress-example
#      namespace: wordpress
#      fieldPath: spec.clusterIP
#      #fieldPath: status.loadBalancer.ingress[0].ip
#      toConnectionSecretKey: ip
#    - apiVersion: v1
#      kind: Service
#      name: wordpress-example
#      namespace: wordpress
#      fieldPath: spec.ports[0].port
#      toConnectionSecretKey: port
#    - apiVersion: v1
#      kind: Secret
#      name: wordpress-example
#      namespace: wordpress
#      fieldPath: data.wordpress-password
#      toConnectionSecretKey: password
#    - apiVersion: v1
#      kind: Secret
#      name: manual-api-secret
#      namespace: wordpress
#      fieldPath: data.api-key
#      toConnectionSecretKey: api-key
#      # this secret created manually (not via Helm chart), so skip 'part of helm release' check
#      skipPartOfReleaseCheck: true
#  writeConnectionSecretToRef:
#    name: wordpress-credentials
#    namespace: crossplane-system
  providerConfigRef:
    name: helm-provider
EOF
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
