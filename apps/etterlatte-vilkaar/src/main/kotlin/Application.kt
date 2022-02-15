package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.model.VilkaarService


fun main() {
    embeddedServer(CIO, applicationEngineEnvironment {
        modules.add { module() }
        connector { port = 8080 }
    }).start(true)
}

fun Application.module() {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
    routing {
        get("/isalive") { call.respondText("ALIVE", ContentType.Text.Plain) }
        get("/isready") { call.respondText("READY", ContentType.Text.Plain) }

        post("/") {
            val vilkaarService = VilkaarService()
            call.respond(
                call.receive<RequestDto>().let {
                    vilkaarService.mapVilkaar(it.opplysninger)
                }
            )
        }

    }
}

data class RequestDto(val opplysninger: List<VilkaarOpplysning<ObjectNode>>)
