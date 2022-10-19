# Event Sourcing Workshop
When how you got there is just as important as the final state.

Event source pattern, is when you store the entire chain of data transformation, ant not just the final state. Each event is immutable, and describes a transformation between two states.

This workshop based on integration with [EventstoreDB](https://www.eventstore.com/eventstoredb).
Going through a sequence of tasks, we will try writing and reading events, and play with projection.

This workshop is based on a simple API (Rest) module written in [Kotlin](https://kotlinlang.org/), with use of [Spring](https://spring.io/).
We will use one the official java client [Maven Central](https://central.sonatype.dev/artifact/com.eventstore/db-client-java/3.0.1/versions) / [Github](https://github.com/EventStore/EventStoreDB-Client-Java), when completing upcoming tasks. 
There is also available clients, such as C#, Go, JavasScript, Rust and TypeScript. More details at [clients](https://developers.eventstore.com/clients/grpc/#connection-details)

For each task, there will be a set of tests that will verify your code. 

## Prerequisites
You need to bring your own PC, with your favorite OS. 
We will write Kotlin kode, so Java is required at version 17 or grater.
[Maven](https://maven.apache.org/) is used as build tool, and at the end we will use Docker and [Docker Compose](https://docs.docker.com/compose/) to get our hands dirty with EventstoreDB GUI. 

Important! As for the workshop, you will get access to a Wifi Network. It might not be as fast as you would like. Download this repo in advanced and build this project, and start docker. This will download all required dependencies, in advanced.
```cmd
## checkout repo
git clone git@github.com:visito/eventstore-wrokshop.git
cd eventstore-workshop

## Build project with maven
mvn clean install

## Start docker. This will download required images
docker compose up -d
## Shutdown resources, and relax. We have you coverd for workshop
docker compose down
```

## Task 1; write events
Start by check out branch *task1*

In this workshop we will work with a bank account model. We will write three different event types:
- Created
- Deposit
- Withdrawal

You find these events in package: org.demo.eventstoredb.eventstore.events

Inside EventstoreRepo, you find functions that need to be updated with help from you.

First we start with *createAccount*. Use available client and 
