<p align="center">
    <a href="https://github.com/halkyonio/primaza-poc/graphs/contributors" alt="Contributors">
        <img src="https://img.shields.io/github/contributors/halkyonio/primaza-poc"/></a>
    <a href="https://github.com/halkyonio/primaza-poc/pulse" alt="Activity">
        <img src="https://img.shields.io/github/commit-activity/m/halkyonio/primaza-poc"/></a>
    <a href="https://github.com/halkyonio/primaza-poc/actions/workflows/push.yml" alt="Build Status">
        <img src="https://github.com/halkyonio/primaza-poc/actions/workflows/push.yaml/badge.svg"></a>
</p>

# Primaza project

Quarkus Primaza Service Box Application - POC

Application developers need access to backing services to build and connect workloads.
Today in Kubernetes, the exposure of secrets for connecting workloads to external services such as REST APIs, databases, event buses, and many more is manual and custom-made.
Connecting workloads to backing services is always a challenge because each service provider suggests a different way to access their secrets, and each application developer consumes those secrets in a custom way to their workloads.
Primaza aims to support the discoverability, life cycle management and connectivity of services running in Kubernetes.

Primaza comes from the Greek word πρυμάτσα, which is a line used to tie boats to the dock.

Primaza exposes several endpoints to manage a few type of objects:

- Claim: Claims should provide a mechanism for application developers to express which Service their applications require without having to know the exact coordinates (group kind version) of the Service Resource.
- Service: represents things like databases, message queues, DNS records, etc.
- Clusters: represent a Kubernetes cluster

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
cd servicebox-app
./mvnw compile quarkus:dev
```

## Usage

Go to http://localhost:8080 in your browser, the page has a few buttons that displays the lists of `Claims`, `Services` and `Clusters`. 
From each specific resource page you have the possibility to create them using the corresponding forms.


### Usage via `curl`

If you wish to send create some resources via `curl` here there are some examples:

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
--data '{"name":"Oracle", "version": "3.11.2", "endpoint": "tcp:5672", "deployed": "false"}' \
http://localhost:8080/services
```

