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


class EventstoreService(private val client: EventStoreDBClient = EventStoreDBClient.create(EventStoreDBClientSettings.builder().addHost(Endpoint("eventstore", 2113)).tls(false).tlsVerifyCert(false).buildConnectionSettings())) {

    private val BY_CATEGORY_STREAM_NAME = "\$ce-account"
    private val STREAM_NAME_PREFIX = "account-"
    private val EVENTS_PACKAGE = Event::class.java.`package`.name

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

        val events = readResult.events.map { toEvent(it.event) }.toList()

        return toAccount(accountID, events)
    }

/*    fun getAccounts(): Accounts {
        val readStreamOptions = ReadStreamOptions.get()
            .fromStart()
            .fromStart()
            .resolveLinkTos()

        val readResult = client
            .readStream(BY_CATEGORY_STREAM_NAME, 10, readStreamOptions)
            .get()

        for (resolvedEvent in readResult.events) {
            val recordedEvent = resolvedEvent.event
            println(toEvent(recordedEvent))
        }

        return Accounts(readResult.events.filter { e -> e.event.eventType.equals("created",true) }.map { e -> toEvent(e.event) }.toList())
    }*/

    private fun toEvent(recordedEvent: RecordedEvent): Event {
        val clazz = Class.forName("$EVENTS_PACKAGE.${recordedEvent.eventType}")
        val event = ObjectMapper().registerKotlinModule()
            .readValue(recordedEvent.eventData, clazz)
        if(event is Event)
            return event
        else throw RuntimeException("Ukjent evnet type")
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

    fun handle(event: Event) {
        when(event){
            is Created -> id = event.id
            is Withdrawal -> amount -= event.amount
            is Deposit -> amount += event.amount
        }
    }

    fun build() = Account(this.id ?: throw RuntimeException("Account not propperly initialized"), this.amount)
}