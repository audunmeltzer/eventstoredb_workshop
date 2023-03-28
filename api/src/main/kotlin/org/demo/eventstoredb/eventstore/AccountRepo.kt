package org.demo.eventstoredb.eventstore

import com.eventstore.dbclient.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.demo.eventstoredb.eventstore.events.Event
import org.springframework.stereotype.Component

const val STREAM_NAME = "account"
private val EVENTS_PACKAGE = Event::class.java.`package`.name

@Component
class EventstoreRepo(private val client: EventStoreDBClient) {

    fun createAccount(accountID: String, name: String): WriteResult {
        //TODO Create AccountCreated event, and use eventstore Client to write event to EvenstoreDB
    }

    fun deposit(accountId: String, description: String, amount: Long): WriteResult {
        //TODO Create Deposit event, and use eventstore Client to write event to EvenstoreDB
    }

    fun withdrawal(accountId: String, description: String, amount: Long) {
        //TODO Create Withdrawl event, and use eventstore Client to write event to EvenstoreDB
    }

    fun getAccount(accountID: String): AccountAggregate? =
        readEventsFromStream(getStreamNameFromId(accountID))
            ?.let { return readResultToAccountAggregate(it) }


    private fun readEventsFromStream(streamName: String): ReadResult? {
        //TODO read all events from stream with name $streamName
        return null
    }

    private fun readResultToAccountAggregate(readResult: ReadResult): AccountAggregate {
        val events = readResult.events.map { it.event.toEvent() }.toList()

        return AccountAggregate().also { aggregate ->
            events.forEach { event -> aggregate.handle(event) }
        }
    }

    private fun getStreamNameFromId(accountID: String) = "$STREAM_NAME-$accountID"


}


fun RecordedEvent.toEvent(): Event {
    val clazz = Class.forName("$EVENTS_PACKAGE.${this.eventType}")
    val event = ObjectMapper().registerKotlinModule()
        .readValue(this.eventData, clazz)
    if(event is Event)
        return event
    else throw RuntimeException("Unknown Event type $clazz")
}