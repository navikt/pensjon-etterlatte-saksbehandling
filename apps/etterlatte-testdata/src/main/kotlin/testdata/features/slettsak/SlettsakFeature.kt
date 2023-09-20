package no.nav.etterlatte.testdata.features

import io.ktor.server.application.call
import io.ktor.server.mustache.MustacheContent
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.logger
import no.nav.etterlatte.navIdentFraToken
import no.nav.etterlatte.producer
import no.nav.etterlatte.testdata.JsonMessage

object SlettsakFeature : TestDataFeature {
    override val beskrivelse: String
        get() = "Slett sak"
    override val path: String
        get() = "slettsak"
    override val routes: Route.() -> Unit
        get() = {
            get {
                val model =
                    mapOf(
                        "beskrivelse" to beskrivelse,
                        "path" to path,
                    )

                call.respond(MustacheContent("slett/slett-sak.hbs", model))
            }

            post {
                val sakId = requireNotNull(call.receiveParameters()["sakId"]).toLong()

                try {
                    val navIdent =
                        requireNotNull(navIdentFraToken()) {
                            "Nav ident mangler. Du må være innlogget for å slette en sak."
                        }

                    val (partisjon, offset) =
                        producer.publiser(
                            sakId.toString(),
                            JsonMessage.newMessage(
                                mapOf(
                                    "@event_name" to "VEDLIKEHOLD:SLETT_SAK",
                                    "sakId" to sakId,
                                ),
                            ).toJson(),
                            mapOf("NavIdent" to (navIdent.toByteArray())),
                        )
                    logger.info("Publiserer melding med partisjon: $partisjon offset: $offset")

                    call.respondRedirect("/$path/slettet?sakId=$sakId&partisjon=$partisjon&offset=$offset")
                } catch (e: Exception) {
                    logger.error("En feil har oppstått! ", e)

                    call.respond(
                        MustacheContent(
                            "error.hbs",
                            mapOf("errorMessage" to e.message, "stacktrace" to e.stackTraceToString()),
                        ),
                    )
                }
            }

            get("slettet") {
                val model =
                    call.request.queryParameters.let {
                        mapOf(
                            "path" to path,
                            "beskrivelse" to beskrivelse,
                            "sakId" to it["sakId"]!!,
                            "partisjon" to it["partisjon"]!!,
                            "offset" to it["offset"]!!,
                        )
                    }

                call.respond(MustacheContent("slett/sletting-sendt.hbs", model))
            }
        }
}
