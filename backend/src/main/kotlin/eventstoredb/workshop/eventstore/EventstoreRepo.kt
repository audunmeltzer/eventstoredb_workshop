package eventstoredb.workshop.eventstore

import com.eventstore.dbclient.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import eventstoredb.workshop.eventstore.events.Created
import eventstoredb.workshop.eventstore.events.Deposit
import eventstoredb.workshop.eventstore.events.Event
import eventstoredb.workshop.eventstore.events.Withdrawal

const val STREAM_NAME_PREFIX = "account"
private val EVENTS_PACKAGE = Event::class.java.`package`.name
const val NO_STREAM = -1L

class EventstoreRepo(private val client: EventStoreDBClient) {

    fun createAccount(accountID: String, name: String) {
        val event = Created(id = accountID, name)
        client.appendToStream("$STREAM_NAME_PREFIX-$accountID",
            AppendToStreamOptions.get().expectedRevision(NO_STREAM),
            EventData.builderAsJson(event::class.java.simpleName, event).build()
        ).get()
    }

    fun deposit(accountId: String, description: String, amount: Long) {
        val event = Deposit(amount = amount, description = description)
        client.appendToStream(
            "$STREAM_NAME_PREFIX-$accountId",
            EventData.builderAsJson(
                event::class.java.simpleName,
                event
            ).build()
        ).get()
    }

    fun withdrawal(accountId: String, description: String, amount: Long) {
        val event = Withdrawal(amount = amount, description = description)
        client.appendToStream(
            "$STREAM_NAME_PREFIX-$accountId",
            EventData.builderAsJson(
                event::class.java.simpleName,
                event
            ).build()
        ).get()
    }

    fun getAccount(accountID: String): AccountAggregate {
        val readStreamOptions = ReadStreamOptions.get()
            .fromStart()
            .fromStart()
            .resolveLinkTos()

        val readResult = client
            .readStream("$STREAM_NAME_PREFIX-$accountID", readStreamOptions)
            .get()

        val events = readResult.events.map { it.event.toEvent() }.toList()

        return AccountAggregate().also {aggregate ->
            events.forEach { event -> aggregate.handle(event) }
        }
    }



}


fun RecordedEvent.toEvent(): Event {
    val clazz = Class.forName("$EVENTS_PACKAGE.${this.eventType}")
    val event = ObjectMapper().registerKotlinModule()
        .readValue(this.eventData, clazz)
    if(event is Event)
        return event
    else throw RuntimeException("Unknown Event type $clazz")
}