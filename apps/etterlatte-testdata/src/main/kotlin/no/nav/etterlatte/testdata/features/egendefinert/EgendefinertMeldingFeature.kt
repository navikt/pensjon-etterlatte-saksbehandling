package no.nav.etterlatte.testdata.features.egendefinert

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

object EgendefinertMeldingFeature : TestDataFeature {
    override val beskrivelse: String
        get() = "Post egendefinert melding"
    override val path: String
        get() = "egendefinert"
    override val routes: Route.() -> Unit
        get() = {
            get {
                call.respond(
                    MustacheContent(
                        "egendefinert/ny-melding.hbs",
                        mapOf(
                            "path" to path,
                            "beskrivelse" to beskrivelse,
                        ),
                    ),
                )
            }

            post {
                try {
                    val navIdent =
                        requireNotNull(navIdentFraToken()) {
                            "Nav ident mangler. Du må være innlogget for å sende søknad."
                        }

                    val (partisjon, offset) =
                        call.receiveParameters().let {
                            producer.publiser(
                                requireNotNull(it["key"]),
                                JsonMessage(requireNotNull(it["json"])).toJson(),
                                mapOf("NavIdent" to navIdent.toByteArray()),
                            )
                        }
                    logger.info("Publiserer melding med partisjon: $partisjon offset: $offset")

                    call.respondRedirect("/$path/sendt?partisjon=$partisjon&offset=$offset")
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

            get("sendt") {
                val partisjon = call.request.queryParameters["partisjon"]!!
                val offset = call.request.queryParameters["offset"]!!

                call.respond(
                    MustacheContent(
                        "egendefinert/melding-sendt.hbs",
                        mapOf(
                            "path" to path,
                            "beskrivelse" to beskrivelse,
                            "partisjon" to partisjon,
                            "offset" to offset,
                        ),
                    ),
                )
            }
        }
}
