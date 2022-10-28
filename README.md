<p align="center">
    <a href="https://github.com/halkyonio/primaza-poc/graphs/contributors" alt="Contributors">
        <img src="https://img.shields.io/github/contributors/halkyonio/primaza-poc"/></a>
    <a href="https://github.com/halkyonio/primaza-poc/pulse" alt="Activity">
        <img src="https://img.shields.io/github/commit-activity/m/halkyonio/primaza-poc"/></a>
    <a href="https://github.com/halkyonio/primaza-poc/actions/workflows/push.yml" alt="Build Status">
        <img src="https://github.com/halkyonio/primaza-poc/actions/workflows/push.yaml/badge.svg"></a>
</p>

# Primaza project

Quarkus Primaza Service box Application - POC

Primaza comes from the Greek word πρυμάτσα, which is a line used to tie boats to the dock.

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
cd servicebox-app
./mvnw compile quarkus:dev
```

## Requests samples

curl --header "Content-Type: application/json" \
--request POST \
--data '{"name":"Oracle"}' \
http://localhost:8080/claims


curl --header "Content-Type: application/json" \                                                                                                     ✔  16:22:00
--request POST \
--data '{"name":"Oracle", "version": "3.11.2", "endpoint": "tcp:5672", "deployed": "false"}' \
http://localhost:8080/services

curl --header "Content-Type: application/json" \
--request POST \
--data '{"name":"Oracle", "version": "3.11.2", "endpoint": "tcp:5672", "deployed": "false"}' \
http://localhost:8080/services

curl --header "Content-Type: application/x-www-form-urlencoded" \
--header "HX-Request: true" \
--request POST \
--data '{"name":"Oracle", "version": "3.11.2", "endpoint": "tcp:5672", "deployed": "false"}' \
http://localhost:8080/services

curl -X POST http://localhost:8080/claims --header "Content-Type: application/json" \
--request POST \
--data "{\"name\":\"Oracle\"}"




> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/servicebox-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Related Guides

- RESTEasy Reactive ([guide](https://quarkus.io/guides/resteasy-reactive)): A JAX-RS implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.

## Provided Code

### RESTEasy Reactive

Easily start your Reactive RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)


