package no.nav.etterlatte.testdata.features.egendefinert

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.ktor.server.application.call
import io.ktor.server.mustache.MustacheContent
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.logger
import no.nav.etterlatte.no.nav.etterlatte.testdata.kunEtterlatteUtvikling
import no.nav.etterlatte.producer
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.OmregningData
import no.nav.etterlatte.testdata.JsonMessage

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
                kunEtterlatteUtvikling {
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
                kunEtterlatteUtvikling {
                    try {
                        val navIdent =
                            requireNotNull(brukerTokenInfo.ident()) {
                                "Nav ident mangler. Du må være innlogget for å sende søknad."
                            }

                        val params = call.receiveParameters()
                        val key = requireNotNull(params["key"])
                        val hendelseType = requireNotNull(params["hendelseType"])
                        val json = requireNotNull(params["json"])

                        if (hendelseType == "omregning") {
                            val jsonNode = objectMapper.readTree(json)
                            jsonNode.get(HENDELSE_DATA_KEY).let {
                                val omregningData: OmregningData = objectMapper.treeToValue(it)
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
                kunEtterlatteUtvikling {
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
