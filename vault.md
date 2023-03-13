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
- Creating a new key under kv path
```bash
kubectl -n vault exec vault-0 --  vault kv put kv/hello target=world
kubectl -n vault exec vault-0 --  vault kv get kv/hello
kubectl -n vault exec vault-0 --  vault kv delete kv/hello
```


### Using vault custom script and vault cli

There is a script in the script folder ready to install and configure vault.

1 - Install vault in the kind cluster.

Go to the scripts folder and launch the script from its location:
````bash
./vault.sh
````

Copy the Root Token shown in the output.

2- Authenticate as Root.
````bash
vault login
````
Note: paste the Root Token when prompted.


3- Create a policy giving access to the path that will stock the secrets
```bash
cat <<EOF | vault policy write vault-quickstart-policy -                                                                                                                  ✔  13:07:05
path "kv/*" {
  capabilities = ["read", "create"]
}
EOF
```

4- Enable the userpass auth secret engine, and create user bob with access to the vault-quickstart-policy
````bash
vault auth enable userpass
vault write auth/userpass/users/bob password=sinclair policies=vault-quickstart-policy
````

5- Logging in using `bob` user:

```bash
vault login -method=userpass username=bob password=sinclair
```

6- Try to create a secret:

```bash
vault kv put kv/hello target=world
```

## To uninstall it
```bash
helm uninstall vault -n vault
kubectl delete pvc -n vault -lapp.kubernetes.io/name=vault
```