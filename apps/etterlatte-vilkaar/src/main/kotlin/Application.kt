package no.nav.etterlatte

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
import no.nav.etterlatte.vilkaar.barnepensjon.brukerErUngNok
import no.nav.etterlatte.vilkaar.model.Opplysning
import no.nav.etterlatte.vilkaar.model.objectMapper

val vilkaarMap = mapOf("barnepensjon:brukerungnok" to brukerErUngNok)

fun main() {
    embeddedServer(CIO, applicationEngineEnvironment {
        modules.add{ module() }
        connector { port = 8080 }
    }).start(true)
}

fun Application.module(){
    install(ContentNegotiation) {
        jackson{
            registerModule(JavaTimeModule())
        }
    }
    routing {
        get("/isalive"){ call.respondText("ALIVE", ContentType.Text.Plain) }
        get("/isready"){ call.respondText("READY", ContentType.Text.Plain) }
        get("/"){ call.respond(brukerErUngNok) }
        post("/") { call.respond(objectMapper.valueToTree(call.receive<RequestDto>().let{vilkaarMap[it.vilkaar]?.vurder(it.opplysninger)?.serialize() }))}
    }

}

data class RequestDto(val vilkaar: String, val opplysninger:List<Opplysning>)
