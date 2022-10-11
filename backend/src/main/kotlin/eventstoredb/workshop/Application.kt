package eventstoredb.workshop

import com.eventstore.dbclient.Endpoint
import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBClientSettings
import eventstoredb.workshop.model.Account
import eventstoredb.workshop.eventstore.EventstoreRepo
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable


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

        get ("/account") {
            call.application.environment.log.info("GET request ${call.request.uri}")

            call.respond(emptyList<Account>())
        }

        post ("/account") {
            call.application.environment.log.info("POST request ${call.request.uri}")
            val payload = call.receive<CreateAccountPayload>()
            call.application.environment.log.info("Post with of payload: $payload")

            eventstoreService.createAccount(payload.AccountID, "Demo Account")

            call.respond(ProducerResponse("Account Created"))
        }

        post ("/account/{account}") {
            call.application.environment.log.info("POST request ${call.request.uri}")
            val accountId = call.parameters["account"]
            val payload = call.receive<AccountPayload>()
            call.application.environment.log.info("Post with of payload: $payload")

            eventstoreService.deposit(accountId!!, "Salary", payload.amount.toLong())

            call.respond(ProducerResponse("Transaction registered"))
        }
    }
}

val eventstoreService = EventstoreRepo(EventStoreDBClient.create(EventStoreDBClientSettings.builder().addHost(Endpoint("eventstore", 2113)).tls(false).tlsVerifyCert(false).buildConnectionSettings()))


@Serializable
data class ProducerResponse(val message: String)
@Serializable
data class AccountPayload(val type: String, val amount: String)

@Serializable
data class CreateAccountPayload(val AccountID: String)

@Serializable
data class Accounts(val accounts: List<Account>)

