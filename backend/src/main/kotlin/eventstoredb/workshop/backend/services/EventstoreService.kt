package eventstoredb.workshop.backend.services

import com.eventstore.dbclient.*
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import eventstoredb.workshop.backend.Accounts
import eventstoredb.workshop.backend.events.Created
import eventstoredb.workshop.backend.events.Deposit
import eventstoredb.workshop.backend.events.Withdrawal
import java.time.Instant


class EventstoreService(private val client: EventStoreDBClient = EventStoreDBClient.create(EventStoreDBClientSettings.builder().addHost(Endpoint("eventstore", 2113)).tls(false).tlsVerifyCert(false).buildConnectionSettings())) {



    fun writeTransaction(accountId: String, type: String, amount: Long) {
        when (type) {
            "Withdrawal" -> {
                client.appendToStream(
                    "bank.account-$accountId",
                    EventData.builderAsJson(
                        "withdrawal",
                        Withdrawal(amount = amount, timestamp = Instant.now().toEpochMilli())
                    ).build()
                ).get()
            }

            "Deposit" -> {
                client.appendToStream(
                    "bank.account-$accountId",
                    EventData.builderAsJson(
                        "deposit",
                        Deposit(amount = amount, timestamp = Instant.now().toEpochMilli())
                    ).build()
                ).get()
            }
        }
    }

    fun createAccount(accountID: String) {
        client.appendToStream("bank.account-$accountID",
            EventData.builderAsJson("created", Created(id = accountID, timestamp = Instant.now().toEpochMilli())).build()
        ).get()
    }

    fun getAccounts(): Accounts {
        val readStreamOptions = ReadStreamOptions.get()
            .fromStart()
            .fromStart()
            .resolveLinkTos()

        val readResult = client
            .readStream("\$ce-bank.account", 10, readStreamOptions)
            .get()

        for (resolvedEvent in readResult.events) {
            val recordedEvent = resolvedEvent.event
            println(mapToEvent(recordedEvent))
        }

        return Accounts(readResult.events.filter { e -> e.event.eventType.equals("created",true) }.map { e -> mapToEvent(e.event) }.toList())
    }

    private fun mapToEvent(recordedEvent: RecordedEvent): Created =
        ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).registerKotlinModule()
            .readValue(recordedEvent.eventData, Created::class.java)
}