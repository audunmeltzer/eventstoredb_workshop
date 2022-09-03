package eventstoredb.workshop.events.producer

import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
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
    route("/producer") {
        get {
            call.application.environment.log.info("GET request ${call.request.uri}")
            call.respond(ProducerResponse("Producer is running!"))
        }
    }
}

@Serializable
data class ProducerResponse(val message: String)

