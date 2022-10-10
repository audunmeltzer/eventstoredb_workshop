package eventstoredb.workshop.backend.services

import com.eventstore.dbclient.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import eventstoredb.workshop.backend.events.Created
import eventstoredb.workshop.backend.events.Deposit
import eventstoredb.workshop.backend.events.Event
import eventstoredb.workshop.backend.events.Withdrawal
import eventstoredb.workshop.backend.model.Account
import io.ktor.server.plugins.*
import java.time.Instant

val STREAM_NAME_PREFIX = "account"
private val EVENTS_PACKAGE = Event::class.java.`package`.name


class EventstoreService(private val client: EventStoreDBClient = EventStoreDBClient.create(EventStoreDBClientSettings.builder().addHost(Endpoint("eventstore", 2113)).tls(false).tlsVerifyCert(false).buildConnectionSettings())) {

    fun deposit(accountId: String, amount: Long) {
        val event = Deposit(amount = amount, timestamp = Instant.now().toEpochMilli())
        client.appendToStream(
            "$STREAM_NAME_PREFIX-$accountId",
            EventData.builderAsJson(
                event::class.java.simpleName,
                event
            ).build()
        ).get()
    }

    fun withdrawal(accountId: String, amount: Long) {
        val event = Withdrawal(amount = amount, timestamp = Instant.now().toEpochMilli())
        client.appendToStream(
            "$STREAM_NAME_PREFIX-$accountId",
            EventData.builderAsJson(
                event::class.java.simpleName,
                event
            ).build()
        ).get()
    }

    fun createAccount(accountID: String) {
        val event = Created(id = accountID, timestamp = Instant.now().toEpochMilli())
        client.appendToStream("$STREAM_NAME_PREFIX-$accountID",
            EventData.builderAsJson(event::class.java.simpleName, event).build()
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

        val accountBuilder = AccountBuilder()
        events.forEach { event -> accountBuilder.handle(event) }
        return accountBuilder.build()
    }
}

class AccountBuilder() {
    var id: String? = null
    var amount: Long = 0L

    fun handle(event: Event) = apply {
        when(event){
            is Created -> id = event.id
            is Withdrawal -> amount -= event.amount
            is Deposit -> amount += event.amount
        }
    }

    fun build() = Account(this.id ?: throw RuntimeException("Account not propperly initialized"), this.amount)
}

fun RecordedEvent.toEvent(): Event {
    val clazz = Class.forName("$EVENTS_PACKAGE.${this.eventType}")
    val event = ObjectMapper().registerKotlinModule()
        .readValue(this.eventData, clazz)
    if(event is Event)
        return event
    else throw RuntimeException("Ukjent evnet type")
}