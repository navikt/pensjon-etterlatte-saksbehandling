package no.nav.etterlatte.hendelserpdl

import io.ktor.http.ContentType
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.etterlatte.stream

@Suppress("unused") // Referenced in application.conf
fun Application.module() {

    routing {
        get("/") {
            call.respondText(
                "Environment: " + System.getenv().keys.joinToString(","),
                contentType = ContentType.Text.Plain
            )
        }
        get("/start") {
            stream?.start()
            call.respondText("Starting leesah stream", contentType = ContentType.Text.Plain)
        }
        get("/status") {
            call.respondText(
                "Iterasjoner: ${stream?.iterasjoner}, Dødsmeldinger ${stream?.dodsmeldinger} av ${stream?.meldinger}",
                contentType = ContentType.Text.Plain
            )
        }
        get("/stop") {
            stream?.stop()
            call.respondText("Stopped reading messages", contentType = ContentType.Text.Plain)
        }

        get("/fromstart") {
            stream?.fraStart()
            call.respondText("partition has been set to start", contentType = ContentType.Text.Plain)
        }

        get("/isAlive") {
            call.respondText("JADDA!", contentType = ContentType.Text.Plain)
        }
        get("/isReady") {
            call.respondText("JADDA!", contentType = ContentType.Text.Plain)
        }
    }
}
