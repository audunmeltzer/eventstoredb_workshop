package eventstoredb.workshop.backend

import com.eventstore.dbclient.*
import com.fasterxml.jackson.databind.ObjectMapper
import eventstoredb.workshop.backend.events.Created
import eventstoredb.workshop.backend.events.Deposit
import eventstoredb.workshop.backend.events.Withdrawal
import eventstoredb.workshop.backend.services.EventstoreService
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

            call.respond(eventstoreService.getAccounts())
        }

        post ("/account") {
            call.application.environment.log.info("POST request ${call.request.uri}")
            val payload = call.receive<CreateAccountPayload>()
            call.application.environment.log.info("Post with of payload: $payload")

            eventstoreService.createAccount(payload.AccountID)

            call.respond(ProducerResponse("Account Created"))
        }

        post ("/account/{account}") {
            call.application.environment.log.info("POST request ${call.request.uri}")
            val accountId = call.parameters["account"]
            val payload = call.receive<AccountPayload>()
            call.application.environment.log.info("Post with of payload: $payload")

            eventstoreService.writeTransaction(accountId!!, payload.type, payload.amount.toLong())

            call.respond(ProducerResponse("Transaction registered"))
        }
    }
}

val eventstoreService = EventstoreService()


@Serializable
data class ProducerResponse(val message: String)
@Serializable
data class AccountPayload(val type: String, val amount: String)

@Serializable
data class CreateAccountPayload(val AccountID: String)

@Serializable
data class Accounts(val accounts: List<Created>)

