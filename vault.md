## How to use Vault

- Steps to follow to install vault on a kind cluster
```bash
helm repo add hashicorp https://helm.releases.hashicorp.com

cat <<EOF > tmp/my-values.yml
server:
  ha:
    enabled: false
  ingress:
    enabled: true
    ingressClassName: nginx    
    hosts:
    - host: vault.127.0.0.1.nip.io
      paths: []
ui:
  enabled: true
  serviceType: "ClusterIP"
EOF
helm install vault hashicorp/vault --create-namespace -n vault -f tmp/my-values.yml
```
- To init and unseal the keys, execute this command
```bash
kubectl -n vault exec vault-0 -- vault operator init \
    -key-shares=1 \
    -key-threshold=1 \
    -format=json > tmp/cluster-keys.json
```
- To unseal
```bash
VAULT_UNSEAL_KEY=$(jq -r ".unseal_keys_b64[]" tmp/cluster-keys.json)
kubectl -n vault exec vault-0 -- vault operator unseal $VAULT_UNSEAL_KEY
```
- To get the `root token`
```bash
jq -r ".root_token" tmp/cluster-keys.json
```
- To uninstall it
```bash
helm uninstall vault -n vault
kubectl delete pvc -n vault -lapp.kubernetes.io/name=vault
```