package no.nav.etterlatte.hendelserpdl // ktlint-disable filename

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID

fun Application.module(stream: LyttPaaHendelser) {
    install(CallLogging) {
        level = org.slf4j.event.Level.INFO
        filter { call -> !call.request.path().matches(Regex(".*/isready|.*/isalive|.*/metrics")) }
        format { call -> "<- ${call.response.status()?.value} ${call.request.httpMethod.value} ${call.request.path()}" }
        mdc(CORRELATION_ID) { call -> call.request.header(X_CORRELATION_ID) ?: java.util.UUID.randomUUID().toString() }
    }

    routing {
        get("/start") {
            stream.start()
            call.respondText("Starting leesah stream", contentType = ContentType.Text.Plain)
        }
        get("/status") {
            call.respondText(
                "Iterasjoner: ${stream.getAntallIterasjoner()}, " +
                    "Dødsmeldinger ${stream.getAntallDoedsMeldinger()}" +
                    " av ${stream.getAntallMeldinger()}",
                contentType = ContentType.Text.Plain
            )
        }
        get("/stop") {
            stream.stop()
            call.respondText("Stopped reading messages", contentType = ContentType.Text.Plain)
        }

        get("/fromstart") {
            stream.fraStart()
            call.respondText("partition has been set to start", contentType = ContentType.Text.Plain)
        }
    }
}