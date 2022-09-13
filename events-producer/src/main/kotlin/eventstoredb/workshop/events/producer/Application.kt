package eventstoredb.workshop.events.producer

import com.eventstore.dbclient.*
import eventstoredb.workshop.events.producer.events.Deposit
import eventstoredb.workshop.events.producer.events.Withdrawal
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
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
            call.application.environment.log.info("GET request ${call.request.uri}")

            val payload = call.receive<AccountPayload>()

            //val type = call.parameters["type"] ?: throw IllegalArgumentException("Parameter type not found")
            call.application.environment.log.info("Post with of payload: $payload")

            //val amount = call.parameters["amount"] ?: throw IllegalArgumentException("Parameter amount not found")

            val settings = EventStoreDBClientSettings.builder().addHost(Endpoint("eventstore", 2113)).tls(false).tlsVerifyCert(false)
            val client = EventStoreDBClient.create(settings.buildConnectionSettings())

            when(payload.type){
                "Withdrawal" -> {
                    withContext(Dispatchers.IO) {
                        client.appendToStream("workshop.trips",
                            EventData.builderAsJson("withdrawal", Withdrawal(id = UUID.randomUUID(), user = "unknown", amount = payload.amount.toLong())).build()
                        ).get()
                    }
                }
                "Deposit" -> {
                    withContext(Dispatchers.IO) {
                        client.appendToStream("workshop.trips",
                            EventData.builderAsJson("deposit", Deposit(id = UUID.randomUUID(), user = "unknown", amount = payload.amount.toLong())).build()
                        ).get()
                    }
                }
            }

            call.respond(ProducerResponse("Money received"))
        }
    }
}

@Serializable
data class ProducerResponse(val message: String)
@Serializable
data class AccountPayload(val type: String, val amount: String)

