package eventstoredb.workshop.services

import com.eventstore.dbclient.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import eventstoredb.workshop.events.Created
import eventstoredb.workshop.events.Deposit
import eventstoredb.workshop.events.Event
import eventstoredb.workshop.events.Withdrawal
import eventstoredb.workshop.model.Account
import io.ktor.server.plugins.*
import java.time.Instant

const val STREAM_NAME_PREFIX = "account"
private val EVENTS_PACKAGE = Event::class.java.`package`.name


class EventstoreRepo(private val client: EventStoreDBClient) {

    fun createAccount(accountID: String) {
        val event = Created(id = accountID, timestamp = Instant.now().toEpochMilli())
        client.appendToStream("$STREAM_NAME_PREFIX-$accountID",
            EventData.builderAsJson(event::class.java.simpleName, event).build()
        ).get()
    }

    fun deposit(accountId: String, amount: Long) {
        val event = Deposit(amount = amount)
        client.appendToStream(
            "$STREAM_NAME_PREFIX-$accountId",
            EventData.builderAsJson(
                event::class.java.simpleName,
                event
            ).build()
        ).get()
    }

    fun withdrawal(accountId: String, amount: Long) {
        val event = Withdrawal(amount = amount)
        client.appendToStream(
            "$STREAM_NAME_PREFIX-$accountId",
            EventData.builderAsJson(
                event::class.java.simpleName,
                event
            ).build()
        ).get()
    }

    fun getAccount(accountID: String): Account {
        val readStreamOptions = ReadStreamOptions.get()
            .fromStart()
            .fromStart()
            .resolveLinkTos()

        val readResult = client
            .readStream("$STREAM_NAME_PREFIX-$accountID", readStreamOptions)
            .get()

        val events = readResult.events.map { it.event.toEvent() }.toList()

        return toAccount(accountID, events)
    }


    private fun toAccount(id: String, events: List<Event>): Account {
        if(events.isEmpty()) throw NotFoundException("Account with id $id not found")

        val accountBuilder = AccountAggregate()
        events.forEach { event -> accountBuilder.handle(event) }
        return accountBuilder.build()
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