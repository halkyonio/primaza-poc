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

* [Primaza project](#primaza-project)
  * [How to play/demo](#how-to-playdemo)
    * [Using Primaza on a k8s cluster](#using-primaza-on-a-k8s-cluster)
    * [Running the application locally](#running-the-application-locally)
  * [Demo time](#demo-time)
  * [Usage](#usage)
    * [Usage via curl](#usage-via-curl)

# Primaza project

Quarkus Primaza Application - POC

Application developers need access to backing services to build and connect workloads.
Today in Kubernetes, the exposure of secrets for connecting workloads to external services such as REST APIs, databases, event buses, and many more is manual and custom-made.
Connecting workloads to backing services is always a challenge because each service provider suggests a different way to access their secrets, and each application developer consumes those secrets in a custom way to their workloads.
Primaza aims to support the discoverability, life cycle management and connectivity of services running in Kubernetes.

Primaza exposes several endpoints to manage different entities such as:

- Claim: Claims should provide a mechanism for application developers to express which Service their applications require without having to know the exact coordinates (group kind version) of the Service Resource.
- Service: represents things like SQL databases, NoSQL databases, messaging brokers, LDAP server, etc.
- Cluster: represent a Kubernetes cluster and will be used as target environment (dev, test, prod, etc) where services are running

Primaza name comes from the Greek word πρυμάτσα, which is a line used to tie boats to the dock.

**Remark**: This project uses Quarkus, the Supersonic Subatomic Java Framework. If you want to learn more about Quarkus, please visit its website: https://quarkus.io/.

## How to play/demo

To use `primaza`, it is needed to perform a couple of things like:

- Setup a kubernetes cluster
- Deploy a backend service such as MySQL, PostgreSQL, etc on a k8s cluster (minikube, kind, etc)
- Register the backend service using the UI, REST endpoint `http://localhost:8080/services/new`
- Create a claim to bind the backend service to your application `http://localhost:8080/claims/new`

If a match exists between the `claim` request and a `service` available (aka a kubernetes endpoint has been created for the service), then you should be able to
get the secret after a few moment :-)

Primaza, according to the list of the registered services, will scan the different clusters to determine if the services are running according to the servie endpoint definition `protocol:port`.
Such services are defined as `available` and will be used by the matching job like also to join the credentials created using the UI `http://localhost:8080/credentials/new`

Whenever a secret is populated including the URL to access the backend service and credential, then the claim status will change from `Pending` to `Bind`. This can be verified using the 
UI `http://localhost:8080/claims`.

### Using Primaza on a k8s cluster

In order to use Primaza on kubernetes, it is needed first to setup a cluster (kind, minikube, etc) and to install an ingress controller like a docker container registry.
To simplify this process, you can use the following bash script able to setup such environment using [kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation) and [helm](https://helm.sh/docs/helm/helm_install/).

```bash
curl -s -L "https://raw.githubusercontent.com/snowdrop/k8s-infra/main/kind/kind-reg-ingress.sh" | bash -s y latest 0"
```
**Remark**: The kubernetes's version can be changed if you replace `latest` with one of the version supported by kind `1.25, etc` 

Create a namespace and set the context
```bash
kubectl create namespace primaza
kubectl config set-context --current --namespace=primaza
```

Next, build the Quarkus application and provide additional parameters used to build/push the image to the docker registry and to generate a helm chart
```bash
mvn clean install -DskipTests -Dquarkus.container-image.build=true \
    -Dquarkus.container-image.push=true    \
    -Dquarkus.container-image.registry=kind-registry:5000    \
    -Dquarkus.container-image.group=local    \
    -Dquarkus.container-image.tag=latest    \
    -Dquarkus.container-image.insecure=true    \
    -Dquarkus.kubernetes.ingress.host=primaza.<VM_IP>.nip.io
```
**Remark**: Don't forget to define the `<VM_IP>`

Push the image to the kind container
```bash
kind load docker-image kind-registry:5000/local/primaza-app
```
When this is done, you can install the helm chart populated by Quarkus
```bash
helm install --devel primaza-app \
  --dependency-update \
  ./target/helm/kubernetes/primaza-app \
  -n primaza \
  --set app.image=localhost:5000/local/primaza-app:latest
```

To play with the local k8s cluster, you will have to create a [cluster](https://primaza.<VM_IP>.nip.io/clusters) where you will import the `kubeconfig` file
which is available using this command `kind get kubeconfig > local-kind-kubeconfig`

If you prefer to use our bash script playing all the commands defined previously, execute this command:
```bash
VM_IP=<VM_IP> ./scripts/primaza.sh
```

### Running the application locally

You can run your application in dev mode that enables live coding using:
```shell script
cd primaza-app
./mvnw compile quarkus:dev
```

## Demo time

To play with Primaza, you can use the following scenario 

- Launch `primaza` locally and open your browser at the address `http://localhgost:8080/home`
- Register a Service
- Go to the screen of the `available` services. It should be empty as no backend service currently exist
- Install a database backend service (e.g Postgresql)
- Go back to the screen of the `available services`. It should be there
- Create for the registered service a credential
- Install the Quarkus SuperHero application

Everything is in place to claim a Service !
- Create a claim for the registered Service
- Bind the claim to the application (using the bind button part of the application screen)
- Do a search using kubectl that a secret has been populated within the namespace of the demo app
- Do the same to check if deployment has been updated
- Access the Quarkus Super Hero app ;-)

The process to install the Quarkus super hero and postgresql DB has been automated and can be installed using this command:
```bash
./scripts/demo.sh 
```

## Usage

Open the `http://localhost:8080` URL in your browser to access the home page. This pages proposes a few buttons to manage the `Claims`, `Services` and `Clusters`. 
From each specific resource page you have the possibility to create them using the corresponding forms.

### Usage via `curl`

If you wish to create some resources using curl, use one of the following requests:

Adding a Claim

```bash
curl --header "Content-Type: application/json" \
--request POST \
--data '{"name":"Oracle"}' \
http://localhost:8080/claims
```
Adding a service

```bash
curl --header "Content-Type: application/json" \
--request POST \
--data '{"name":"Oracle", "version": "3.11.2", "endpoint": "tcp:5672", "available": "false"}' \
http://localhost:8080/services
```

