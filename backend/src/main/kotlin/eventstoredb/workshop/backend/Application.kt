package eventstoredb.workshop.backend

import com.eventstore.dbclient.*
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import eventstoredb.workshop.backend.events.Created
import eventstoredb.workshop.backend.events.Deposit
import eventstoredb.workshop.backend.events.Withdrawal
import io.ktor.serialization.jackson.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    routing {
        konfigurasjon()
    }
}

fun Route.konfigurasjon() {
    route("/api") {
        get {
            call.application.environment.log.info("GET request ${call.request.uri}")
            call.respond(ProducerResponse("Producer is running!"))
        }


        post ("/account") {
            call.application.environment.log.info("POST request ${call.request.uri}")
            val payload = call.receive<CreateAccountPayload>()
            call.application.environment.log.info("Post with of payload: $payload")


            withContext(Dispatchers.IO) {
                client.appendToStream("bank.account-${payload.AccountID}",
                    EventData.builderAsJson("created", Created(id = payload.AccountID, timestamp = Instant.now().toEpochMilli())).build()
                ).get()
            }


            call.respond(ProducerResponse("Account Created"))
        }

        post ("/account/{account}") {
            call.application.environment.log.info("POST request ${call.request.uri}")
            val accountId = call.parameters["account"]
            val payload = call.receive<AccountPayload>()
            call.application.environment.log.info("Post with of payload: $payload")

            when(payload.type){
                "Withdrawal" -> {
                    withContext(Dispatchers.IO) {
                        client.appendToStream("bank.account-$accountId",
                            EventData.builderAsJson("withdrawal", Withdrawal(amount = payload.amount.toLong(), timestamp = Instant.now().toEpochMilli())).build()
                        ).get()
                    }
                }
                "Deposit" -> {
                    withContext(Dispatchers.IO) {
                        client.appendToStream("bank.account-$accountId",
                            EventData.builderAsJson("deposit", Deposit(amount = payload.amount.toLong(), timestamp = Instant.now().toEpochMilli())).build()
                        ).get()
                    }
                }
            }

            call.respond(ProducerResponse("Transaction registered"))
        }
    }
}
var client = EventStoreDBClient.create(EventStoreDBClientSettings.builder().addHost(Endpoint("eventstore", 2113)).tls(false).tlsVerifyCert(false).buildConnectionSettings())


@Serializable
data class ProducerResponse(val message: String)
@Serializable
data class AccountPayload(val type: String, val amount: String)

@Serializable
data class CreateAccountPayload(val AccountID: String)

