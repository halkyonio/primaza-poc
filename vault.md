## How to use Vault

- Steps to follow to install vault on a kind cluster
```bash
helm repo add hashicorp https://helm.releases.hashicorp.com

cat <<EOF > my-values.yml
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
helm install vault hashicorp/vault --create-namespace -n vault -f my-values.yml
```
- To uninstall it
```bash
helm uninstall vault -n vault
```