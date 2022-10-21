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

### Task 1; write events
Start by check out branch *task1*

In this workshop we will work with a bank account model. We will write three different event types:
- Created
- Deposit
- Withdrawal

You find these events in package: org.demo.eventstoredb.eventstore.events

Inside EventstoreRepo, you find three functions that need to be updated with help from you.
```kotlin
fun createAccount(accountID: String, name: String): WriteResult {
    //TODO Create AccountCreated event, and use eventstore Client to write event to EvenstoreDB
}

fun deposit(accountId: String, description: String, amount: Long): WriteResult {
    //TODO Create Deposit event, and use eventstore Client to write event to EvenstoreDB
}

fun withdrawal(accountId: String, description: String, amount: Long) {
    //TODO Create Withdrawl event, and use eventstore Client to write event to EvenstoreDB
}
```

### Task 2; version control
In this task we will enforce two rules when writing events:
- Account stream should always start with a AccountCreated event.
- AccountCreated should only be written if stream does not exist.

Update your implemententaion in Task 1.

Tip:
```java
public CompletableFuture<WriteResult> appendToStream(String streamName, AppendToStreamOptions options, EventData... events)
```

### Task 3; Time to read
Great! It's time to get something back from our event driven database.
Help us implement 

```kotlin
private fun readEventsFromStream(streamName: String): ReadResult {
    //TODO read all events from stream with name $streamName
}
```

## Event Sourcing and CQRS
Command-Query Segregation is a principal, where you seperate wrtire model from read model. This has two main advantages:
- Both write and read model can be optimized
- Scalability: By dividing our application in one query module, and one write module, we can scale them differently as needed.

We can implement CQRS principal, by project data from EventstoreDB to desired read model. As an example, a read model can be written in memory, SQL database, or NoSQL.

![](/home/audun/github/eventstoredb_workshop/images/cqrs.svg)

````puml
@startuml
cloud client
component Write
component Read
database EventstoreDB
database ReadModel
component Projection

client --> Write : Add / Update
client --> Read : Query
Write --> EventstoreDB : Add / Update
EventstoreDB -> Projection : Subscribe to stream
Projection -> ReadModel : Update readmodel
Read --> ReadModel : Query data
@enduml
````

### Task 4; Project into memory
In this task you will implement a projection which will project all our data into an in memory read model.

Open AccountProjection and implement 
```kotlin
override fun onEvent(subscription: Subscription, resolvedEvent: ResolvedEvent)
```

In this time our read database is in memory, implemented as a map. Each Event should create or update the state in *accounts* 