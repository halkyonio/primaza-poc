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
cat <<EOF | vault policy write vault-primaza-policy -                                                                                                                  ✔  13:07:05
path "kv/primaza/*" {
  capabilities = ["read", "create"]
}
EOF
```

4- Enable the userpass auth secret engine, and create user bob with access to the vault-quickstart-policy
````bash
vault auth enable userpass
vault write auth/userpass/users/bob password=sinclair policies=vault-primaza-policy
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

or 

```bash
./vault.sh remove
```

## Using Dev Services for Vault in Primaza

When testing or running in dev mode Quarkus can provide you with a zero-config Vault out of the box. 
Since the Vault extension is included in the classpath, Quarkus will provide the Vault server inside a Docker container. 
So, to use the Vault provided by Quarkus, start the application in dev mode: 

```bash
mvn quarkus:dev
```

Tape `h` key as indicated in the prompt for more options:  

```bash
The following commands are currently available:

== Continuous Testing

[r] - Resume testing
[o] - Toggle test output (disabled)

== Dev Services

[c] - Show dev services containers
[g] - Follow dev services logs to the console (disabled)

== Exceptions

[x] - Opens last exception in IDE (None)

== HTTP

[w] - Open the application in a browser
[d] - Open the Dev UI in a browser

== System

[s] - Force restart
[i] - Toggle instrumentation based reload (disabled)
--


```
Then tape `c` in order to show the Dev Services containers running. You should see something like:

```bash

== Dev Services

  Injected Config:  quarkus.vault.url=http://localhost:64966, quarkus.vault.authentication.client-token=root

```

If you want to use this Vault in command line for creating or reading secrets, you can configure the address and token as follows:

```bash
export VAULT_TOKEN=root
export VAULT_ADDR=http://localhost:64966 
```

If everything went well, you should be able to create and read secrets:

```bash
vault kv put secret/myapp hello=world
== Secret Path ==
secret/data/myapp

======= Metadata =======
Key                Value
---                -----
created_time       2023-03-24T16:56:22.628685834Z
custom_metadata    <nil>
deletion_time      n/a
destroyed          false
version            1

```

```bash
vault kv get secret/myapp                                                                                                                               ✔  17:56:22 
== Secret Path ==
secret/data/myapp

======= Metadata =======
Key                Value
---                -----
created_time       2023-03-24T16:56:22.628685834Z
custom_metadata    <nil>
deletion_time      n/a
destroyed          false
version            1

==== Data ====
Key      Value
---      -----
hello    world
```