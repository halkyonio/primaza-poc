<p align="center">
    <a href="https://github.com/halkyonio/primaza-poc/graphs/contributors" alt="Contributors">
        <img src="https://img.shields.io/github/contributors/halkyonio/primaza-poc"/></a>
    <a href="https://github.com/halkyonio/primaza-poc/pulse" alt="Activity">
        <img src="https://img.shields.io/github/commit-activity/m/halkyonio/primaza-poc"/></a>
    <a href="https://github.com/halkyonio/primaza-poc/actions/workflows/push.yml" alt="Build Status">
        <img src="https://github.com/halkyonio/primaza-poc/actions/workflows/push.yaml/badge.svg"></a>
</p>

Table of Contents
=================

* [Introduction](#introduction)
* [Primaza initiative](#primaza-initiative)
  * [How it works](#how-it-works)
  * [How to play/demo](#how-to-playdemo)
    * [Running the application locally](#running-the-application-locally)
    * [Using Primaza on a k8s cluster](#using-primaza-on-a-k8s-cluster)
  * [Demo time](#demo-time)

# Introduction

Modern runtime applications designed to be `Cloud Native` must also been able to connect to `backend` systems (SQL, NoSQL, Broker, etc) as this is the case for applications
running on physical or virtual machines.

To access a system, it is needed to configure different resources (e.g. Datasource) and to declare different parameters (e.g jdbc url, username, password, database name) using
`application.properties` able to configure the connection with by example a SQL database.

To avoid to hard code such parameters, we use mostly a kubernetes secret and pass the needed information as a list of key/value pairs

```text
type: postgresql
provider: bitnami
database: fruits_database
uri: postgresql:5432
username: healthy
password: healthy
```

This approach is subject to different problems as:
- Creating a YAML file to pass the information is `error prone`,
- It is sometimes not trivial to figure out for a user, according to the target platforms (dev, test, etc), the parameters,
- It exposes some sensitive data like the password to persons which are not authorized to access a backend system,
- The consumer of the backend service must spend time to discover how the service will or has been installed to collect the parameters and to map them to their `application.properties`
- The Service provider do not use a stadardize way to set the different properties according to a standard. 
- The [Service Binding spec](https://github.com/servicebinding/spec#well-known-secret-entries) do not even recommend how, according to a service type (SQL, etc) the parameters should be named

# Primaza initiative

Primaza name comes from the Greek word πρυμάτσα, which represent a `stern line` used to tie [boats](https://en.wikipedia.org/wiki/Mooring) to the dock.

Primaza aims to support the discoverability, life cycle management & connection of services running in Kubernetes (or outside) with a runtime applications.

Primaza introduces new concepts supporting such a vision:

- Claim: corresponds to a way for an application developer to express which service, version, environment, etc their application requires to access and how they would like to get back the information.
- Service Identity: represents things like a SQL, NoSQL database, messaging brokers, LDAP server, contains metadata information able for Primaza to search/discover service deployed in an environment (e.g. dev cluster)
- Credential: local storage of username & passwords to access a Service or path to access the information to fetch them from an external Secret store engine (e.g. vault)
- Cluster: is a Kubernetes cluster config and will be used as target environment (dev, test, prod, etc) where services should be discovered

## How it works 

To bind a service to a runtime, it is needed to create a Claim CRD. This claim contains the name of the service, its version and optional some additional parameters like: role, target environment.

When the controller detects such a new Claim CRD, then it will populate a request and will call the Primaza Claim REST endpoint.

According to the information received, Primaza will check if a match exists between a `claim` and a `service` registered and will discover it on the target cluster (dev test, etc).

To determine if a kubernetes service is available, Primaza will use the service endpoint definition and more specifically the `protocol:port` of the service to be watched.

Next, the credential associated with the service will be retrieved locally or using a secret store and a secret will be created containing as information:

```text
type     // Service Type: postgresql, mysql
provider // Service provider: bitnami, etc
uri      // kubernetes DNS name and port
username // user to be used to access the service
password // Password to be used to access the service
database // (optional): Database name to connect to a SQL database
```
The secret is crated under the namespace of the application and a volume is crated, part of the application to mount the secret using the [workload projection](https://github.com/servicebinding/spec#workload-projection) convention.

## How to play/demo

To use `primaza`, it is needed to perform a couple of things like:

- Setup a kubernetes cluster
- Deploy a backend service such as MySQL, Postgresql, etc on a k8s cluster (minikube, kind, etc)
- Install Primaza (or run it locally)
- Tell to Primaza how to access the different clusters `http://localhost:8080/clusters/new`
- Define the Service `http://localhost:8080/services/new` to let Primaza to discover it
- Create the Credential or path to access the credential from a secret store `http://localhost:8080/credentials/new`
- Create a claim to bind the backend service to your application `http://localhost:8080/claims/new`

### Running the application locally

You can run the quarkus primaza application in dev mode using the command:
```shell script
cd app
./mvnw compile quarkus:dev
```
The command will launch the runtime at the following address: `http://localhost:8080` 
but will also run different containers:  database (h2) & vault secret engine if your docker or podman daemon is running locally !

You can discover the [quarkus dev services](https://quarkus.io/guides/dev-services) and injected config by pressing on the key `c` within your terminal.

If you plan to play with a quarkus application and bind it to a service, follow then the instructions of the [Demo time](#demo-time) section :-)

### Using Primaza on a k8s cluster

In order to use Primaza on kubernetes, it is needed first to setup a cluster (kind, minikube, etc) and to install an ingress controller.
To simplify this process, you can use the following bash script able to set up such environment using [kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation) and [helm](https://helm.sh/docs/helm/helm_install/).

```bash
VM_IP=<IP_ADDRESS_OF_THE_CLUSTER>
curl -s -L "https://raw.githubusercontent.com/snowdrop/k8s-infra/main/kind/kind-reg-ingress.sh" | bash -s y latest kind 0 ${VM_IP}
```
**Remark**: The kubernetes's version can be changed if you replace `latest` with one of the version supported by kind `1.23 .. 1.25`

Install vault using the following script `./scripts/vault.sh`. We recommend to use this script as it is needed to perform different steps
post vault installation such as: 
- unseal, 
- store root token within the local folder `.vault/cluster-keys.json`, 
- install kv2 secret engine, 
- populate a policy, 
- assign the policy to a user `bob` and password` sinclair`

> **Note**: If creation of the vault's pod is taking more than 60s as the container image must be downloaded, then the process will stop.
In this case, remove the helm chart `./scripts/vault.sh remove` and repeat the operation.

> **Tip**: Notice the messages displayed within the console as they told you how to get the root token and where they are stored, where to access the keys, etc !

Create the primaza namespace
```bash
kubectl create namespace primaza
```
Set the `VAULT_URL` variable to let primaza to access storage engine using the Kubernetes DNS service name:
```bash
export VAULT_URL=http://vault-internal.vault:8200
```
Next, deploy Primaza and its Postgresql DB using the following helm chart
```bash
helm install \
  --devel \
  --repo https://halkyonio.github.io/helm-charts \
  primaza-app \
  primaza-app \
  -n primaza \
  --set app.image=quay.io/halkyonio/primaza-app:latest \
  --set app.host=primaza.${VM_IP}.nip.io \
  --set app.envs.vault.url=${VAULT_URL}
```
> **Tip**: When the pod is started, you can access Primaza using its ingress host url: `http://primaza.<VM_IP>.nip.io`

If you prefer to install everything all-in-one, use our bash scripts on a `kind` k8s cluster:
```bash
VM_IP=<VM_IP>
VAULT_URL=http://vault-internal.vault:8200
$(pwd)/scripts/vault.sh
$(pwd)/scripts/primaza.sh
```
And now, you can demo it ;-)

## Demo time

To play with Primaza, you can use the following scenario: 

- Launch `primaza` locally and open your browser at the address `http://localhost:8080`
- Create the `cluster` and `service` records
- Go to the screen of the `discover`. It should be empty as no backend service currently runs
- Install a database backend service (e.g Postgresql)
- Go back to the screen of the `discover`. It should be there
- Create for the `registered service` its `credential`
- Install the Quarkus Atomic Fruits application
- When the application will start, then it will crash as atomic fruits cannot yet access database
- Create a `claim` to tell to Primaza that the Atomic fruits app would like to access a Posgresql DB
- Select the `Atomic Fruits application` from the screen `applications` and click on the button `claim`
- Select the claim to bind from the list
- If the binding succeeded, then the status should be `bound` and the ingress URL should be displayed

Everything is in place to claim a Service using the following commands:

- Install the postgresql DB that the Quarkus Fruits application will access
  ```bash
  DB_USERNAME=healthy
  DB_PASSWORD=healthy
  DB_DATABASE=fruits_database
  RELEASE_NAME=postgresql
  VERSION=11.9.13
  helm uninstall postgresql -n db
  kubectl delete pvc -lapp.kubernetes.io/name=$RELEASE_NAME -n db
  
  helm install $RELEASE_NAME bitnami/postgresql \
    --version $VERSION \
    --set auth.username=$DB_USERNAME \
    --set auth.password=$DB_PASSWORD \
    --set auth.database=$DB_DATABASE \
    --create-namespace \
    -n db
  ```
- Deploy the Quarkus Fruits application within the namespace `app`
  ```bash
  kubectl create ns app
  kubectl delete -f $(pwd)/scripts/data/atomic-fruits.yml
  kubectl apply -f $(pwd)/scripts/data//atomic-fruits.yml
  ```
- Create an entry within the secret store engine at the path `primaza/fruits` to configure for the username/password
  ```bash
  export VAULT_ADDR=http://localhost:<VAULT_PORT>  // If you run vault locally as a TestContainer, then find its port using the "quarkus dev service"
  export VAULT_ADDR=http://vault.<VM_IP>.nip.io    // If you deploy vault on a k8s cluster
  vault login -method=userpass username=bob password=sinclair // If you use vault on a k8s cluster
  vault kv put -mount=secret primaza/fruits healthy=healthy
  vault kv get -mount=secret primaza/fruits
  ```
- Create now different records to let Primaza to access the local cluster, 
  ```bash
  export PRIMAZA_URL=primaza.<VM_IP>.nip.io
  $(pwd)/scripts/data/cluster.sh
  $(pwd)/scripts/data/services.sh
  $(pwd)/scripts/data/credentials.sh
  $(pwd)/scripts/data/claims.sh
  ```

- Open the browser ate the address: `http://localhost:8080`   
- Click on the different UI screens to verify if the cluster has been well registered and if its status is `OK`, that the application `atomic fruits`, if primaza has discovered the postgresql db
- Select the `Atomic Fruits application` from the screen `applications` and click on the button `claim`
- Select the claim `fruits-claim` to bind from the list and click on the button `bind`
- Wait 1-2 seconds and refresh the screen
- If the binding succeeded, then the status should be `bound` and the ingress URL should be displayed
- Click on the URL to access the application's screen
- Alternatively `curl` or `httpie` the URL (`http http://atomic-fruits.127.0.0.1.nip.io/fruits`)
- Enjoy :-)