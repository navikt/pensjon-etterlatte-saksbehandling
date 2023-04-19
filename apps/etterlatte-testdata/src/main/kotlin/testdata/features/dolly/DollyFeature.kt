package no.nav.etterlatte.testdata.features.dolly

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.application.call
import io.ktor.server.mustache.MustacheContent
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.getClientAccessToken
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.logger
import no.nav.etterlatte.navIdentFraToken
import no.nav.etterlatte.objectMapper
import no.nav.etterlatte.producer
import no.nav.etterlatte.testdata.dolly.BestillingRequest
import no.nav.etterlatte.testdata.dolly.DollyService
import no.nav.etterlatte.usernameFraToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import testdata.features.soeknad.SoeknadMapper.opprettSoeknadJson
import java.util.*

class DollyFeature(private val dollyService: DollyService) : TestDataFeature {
    private val logger: Logger = LoggerFactory.getLogger(DollyFeature::class.java)
    override val beskrivelse: String
        get() = "Opprett søknad automatisk via Dolly"
    override val path: String
        get() = "dolly"

    override val routes: Route.() -> Unit
        get() = {
            get {
                val accessToken = getClientAccessToken()
                logger.info("got accesstoken")
                val gruppeId = dollyService.hentTestGruppe(usernameFraToken()!!, accessToken)

                call.respond(
                    MustacheContent(
                        "dolly/dolly.hbs",
                        mapOf(
                            "beskrivelse" to beskrivelse,
                            "path" to path,
                            "gruppeId" to gruppeId
                        )
                    )
                )
            }

            get("hent-familier") {
                try {
                    val accessToken = getClientAccessToken()
                    val gruppeId = call.request.queryParameters["gruppeId"]!!.toLong()

                    val familier = try {
                        dollyService.hentFamilier(gruppeId, accessToken)
                    } catch (ex: Exception) {
                        logger.error("Klarte ikke hente familier", ex)
                        emptyList()
                    }

                    call.respond(familier.toJson())
                } catch (e: Exception) {
                    logger.error("En feil har oppstått! ", e)
                    call.respond(
                        MustacheContent(
                            "error.hbs",
                            mapOf("errorMessage" to e.message, "stacktrace" to e.stackTraceToString())
                        )
                    )
                }
            }

            post("opprett-familie") {
                call.receiveParameters().let {
                    try {
                        val accessToken = getClientAccessToken()
                        val req = BestillingRequest(
                            it["helsoesken"]!!.toInt(),
                            it["halvsoeskenAvdoed"]!!.toInt(),
                            it["halvsoeskenGjenlevende"]!!.toInt(),
                            it["gruppeId"]!!.toLong()
                        )

                        dollyService.opprettBestilling(generererBestilling(req), req.gruppeId, accessToken)
                            .also { bestilling ->
                                logger.info("Bestilling med id ${bestilling.id} har status ${bestilling.ferdig}")
                                call.respond(bestilling.toJson())
                            }
                    } catch (e: Exception) {
                        logger.error("En feil har oppstått! ", e)
                        call.respond(
                            MustacheContent(
                                "error.hbs",
                                mapOf("errorMessage" to e.message, "stacktrace" to e.stackTraceToString())
                            )
                        )
                    }
                }
            }

            post("send-soeknad") {
                try {
                    val noekkel = UUID.randomUUID().toString()

                    val request = call.receiveParameters().let {
                        NySoeknadRequest(
                            it["type"]!!,
                            it["avdoed"]!!,
                            it["gjenlevende"]!!,
                            objectMapper.readValue(it["barn"] ?: "[]", jacksonTypeRef<List<String>>())
                        )
                    }

                    val (partisjon, offset) = producer.publiser(
                        noekkel,
                        opprettSoeknadJson(
                            type = request.type,
                            gjenlevendeFnr = request.gjenlevende,
                            avdoedFnr = request.avdoed,
                            barn = request.barn
                        ),
                        mapOf("NavIdent" to (navIdentFraToken()!!.toByteArray()))
                    )
                    logger.info("Publiserer melding med partisjon: $partisjon offset: $offset")

                    dollyService.markerSomIBruk(request.avdoed, getClientAccessToken())

                    call.respond(SoeknadResponse(200, noekkel).toJson())
                } catch (e: Exception) {
                    logger.error("En feil har oppstått! ", e)

                    call.respond(
                        MustacheContent(
                            "error.hbs",
                            mapOf("errorMessage" to e.message, "stacktrace" to e.stackTraceToString())
                        )
                    )
                }
            }
        }
}

data class NySoeknadRequest(
    val type: String,
    val avdoed: String,
    val gjenlevende: String,
    val barn: List<String> = emptyList()
)

data class SoeknadResponse(
    val status: Number,
    val noekkel: String
)