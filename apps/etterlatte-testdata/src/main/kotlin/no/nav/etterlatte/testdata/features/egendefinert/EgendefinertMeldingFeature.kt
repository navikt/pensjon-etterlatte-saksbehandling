package no.nav.etterlatte.testdata.features.egendefinert

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.ktor.server.mustache.MustacheContent
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.logger
import no.nav.etterlatte.omregning.OmregningData
import no.nav.etterlatte.producer
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.testdata.JsonMessage
import no.nav.etterlatte.testdata.kunEtterlatte

object EgendefinertMeldingFeature : TestDataFeature {
    override val beskrivelse: String
        get() = "Post egendefinert melding"
    override val path: String
        get() = "egendefinert"
    override val kunEtterlatte: Boolean
        get() = true
    override val routes: Route.() -> Unit
        get() = {
            get {
                kunEtterlatte {
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
            }

            post {
                kunEtterlatte {
                    try {
                        val navIdent =
                            krevIkkeNull(brukerTokenInfo.ident()) {
                                "Nav ident mangler. Du må være innlogget for å sende søknad."
                            }

                        val params = call.receiveParameters()
                        val key = krevIkkeNull(params["key"]) { "Parameter key mangler" }
                        val hendelseType = krevIkkeNull(params["hendelseType"]) { "Parameter hendelseType mangler" }
                        val json = krevIkkeNull(params["json"]) { "Parameter json mangler" }

                        if (hendelseType == "omregning") {
                            val jsonNode = objectMapper.readTree(json)
                            jsonNode.get(HENDELSE_DATA_KEY).let {
                                // Sjekker at vi kan parse egendefinert melding som OmregningData
                                objectMapper.treeToValue<OmregningData>(it)
                            }
                        }

                        val (partisjon, offset) =
                            producer.publiser(
                                key,
                                JsonMessage(json).toJson(),
                                mapOf("NavIdent" to navIdent.toByteArray()),
                            )

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
            }

            get("sendt") {
                kunEtterlatte {
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
}
