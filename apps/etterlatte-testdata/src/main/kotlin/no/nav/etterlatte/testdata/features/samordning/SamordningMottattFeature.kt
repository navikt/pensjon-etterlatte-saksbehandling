package no.nav.etterlatte.testdata.features.samordning

import io.ktor.server.application.call
import io.ktor.server.mustache.MustacheContent
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.logger
import no.nav.etterlatte.producer
import no.nav.etterlatte.testdata.JsonMessage

object SamordningMottattFeature : TestDataFeature {
    override val beskrivelse: String
        get() = "Send melding om mottatt/ferdig samordnet vedtak"
    override val path: String
        get() = "samordningmottatt"
    override val routes: Route.() -> Unit
        get() = {
            get {
                call.respond(
                    MustacheContent(
                        "samordning/mottatt-samordningsmelding.hbs",
                        mapOf(
                            "beskrivelse" to beskrivelse,
                            "path" to path,
                        ),
                    ),
                )
            }

            post {
                try {
                    val navIdent =
                        requireNotNull(brukerTokenInfo.ident()) {
                            "Nav ident mangler. Du må være innlogget for å sende samordningsmelding."
                        }

                    val (partisjon, offset) =
                        call.receiveParameters().let {
                            val vedtakID = requireNotNull(it["vedtakIdInput"])
                            producer
                                .publiser(
                                    requireNotNull(it["key"]),
                                    JsonMessage
                                        .newMessage(
                                            mapOf(
                                                VedtakKafkaHendelseHendelseType.SAMORDNING_MOTTATT.lagParMedEventNameKey(),
                                                "vedtakId" to vedtakID,
                                            ),
                                        ).toJson(),
                                    mapOf("NavIdent" to navIdent.toByteArray()),
                                ).also { (partisjon, offset) ->
                                    logger.info(
                                        "Publiserer samordningsmelding for vedtakID=$vedtakID med partisjon: $partisjon offset: $offset",
                                    )
                                }
                        }

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
                        "samordning/samordningsmelding-sendt.hbs",
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
