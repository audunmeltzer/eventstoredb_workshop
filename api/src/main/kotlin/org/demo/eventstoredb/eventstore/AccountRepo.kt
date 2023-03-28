package org.demo.eventstoredb.eventstore

import com.eventstore.dbclient.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.demo.eventstoredb.eventstore.events.AccountCreated
import org.demo.eventstoredb.eventstore.events.Deposit
import org.demo.eventstoredb.eventstore.events.Event
import org.demo.eventstoredb.eventstore.events.Withdrawal
import org.springframework.stereotype.Component

const val STREAM_NAME = "account"
private val EVENTS_PACKAGE = Event::class.java.`package`.name

@Component
class EventstoreRepo(private val client: EventStoreDBClient) {

    fun createAccount(accountID: String, name: String): WriteResult {
        val event = AccountCreated(id = accountID, name)
        return client.appendToStream(
            getStreamNameFromId(accountID),
            AppendToStreamOptions.get(),
            EventData.builderAsJson(event::class.java.simpleName, event).build()
        ).get()
    }

    fun deposit(accountId: String, description: String, amount: Long): WriteResult {
        val event = Deposit(amount = amount, description = description)
        return client.appendToStream(
            getStreamNameFromId(accountId),
            AppendToStreamOptions.get(),
            EventData.builderAsJson(
                event::class.java.simpleName,
                event
            ).build()
        ).get()
    }

    fun withdrawal(accountId: String, description: String, amount: Long) {
        val event = Withdrawal(amount = amount, description = description)
        client.appendToStream(
            getStreamNameFromId(accountId),
            AppendToStreamOptions.get(),
            EventData.builderAsJson(
                event::class.java.simpleName,
                event
            ).build()
        ).get()
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