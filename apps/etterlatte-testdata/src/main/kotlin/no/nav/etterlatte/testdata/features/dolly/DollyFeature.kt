package no.nav.etterlatte.testdata.features.dolly

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import io.ktor.http.HttpStatusCode
import io.ktor.server.mustache.MustacheContent
import io.ktor.server.request.receive
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondNullable
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.brukerIdFraToken
import no.nav.etterlatte.getDollyAccessToken
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.objectMapper
import no.nav.etterlatte.rapidsandrivers.Behandlingssteg
import no.nav.etterlatte.testdata.dolly.BestillingRequest
import no.nav.etterlatte.testdata.dolly.DollyService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate

class DollyFeature(
    private val dollyService: DollyService,
    private val vedtakService: VedtakService,
) : TestDataFeature {
    private val logger: Logger = LoggerFactory.getLogger(DollyFeature::class.java)
    override val beskrivelse: String
        get() = "Opprett søknad automatisk via Dolly"
    override val path: String
        get() = "dolly"
    override val kunEtterlatte: Boolean
        get() = false

    override val routes: Route.() -> Unit
        get() = {
            get {
                val accessToken = getDollyAccessToken()

                val gruppeId = dollyService.hentTestGruppeId(brukerIdFraToken()!!, accessToken)

                call.respond(
                    MustacheContent(
                        "dolly/dolly.hbs",
                        mapOf(
                            "beskrivelse" to beskrivelse,
                            "path" to path,
                            "gruppeId" to gruppeId,
                        ),
                    ),
                )
            }

            get("hent-familier") {
                try {
                    val accessToken = getDollyAccessToken()
                    val gruppeId = call.request.queryParameters["gruppeId"]!!.toLong()

                    val familier =
                        try {
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
                            mapOf("errorMessage" to e.message, "stacktrace" to e.stackTraceToString()),
                        ),
                    )
                }
            }

            post("opprett-familie") {
                call.receiveParameters().let {
                    try {
                        val accessToken = getDollyAccessToken()
                        val req =
                            BestillingRequest(
                                erOver18 = it["barnOver18"]!!.toBoolean(),
                                helsoesken = it["helsoesken"]!!.toInt(),
                                halvsoeskenAvdoed = it["halvsoeskenAvdoed"]!!.toInt(),
                                halvsoeskenGjenlevende = it["halvsoeskenGjenlevende"]!!.toInt(),
                                gruppeId = it["gruppeId"]!!.toLong(),
                                antall = 1,
                            )

                        dollyService
                            .opprettBestilling(generererBestilling(req), req.gruppeId, accessToken)
                            .also { bestilling ->
                                logger.info("Bestilling med id ${bestilling.id} har status ${bestilling.ferdig}")
                                call.respond(bestilling.toJson())
                            }
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

            post("send-soeknad") {
                try {

                    val request =
                        call.receiveParameters().let {
                            NySoeknadRequest(
                                SoeknadType.valueOf(it["type"]!!),
                                it["avdoed"]!!,
                                it["gjenlevende"]!!,
                                objectMapper.readValue(it["barnListe"] ?: "[]", jacksonTypeRef<List<String>>()),
                            )
                        }

                    val noekkel =
                        dollyService.sendSoeknad(request, brukerTokenInfo.ident(), Behandlingssteg.BEHANDLING_OPPRETTA)

                    call.respond(SoeknadResponse(200, noekkel).toJson())
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

            post("api/v1/opprett-ytelse") {
                try {

                    val nySoeknadRequest = call.receive<NySoeknadRequest>()
                    val ytelse = nySoeknadRequest.type
                    val behandlingssteg = Behandlingssteg.IVERKSATT
                    val gjenlevende = nySoeknadRequest.gjenlevende
                    val avdoed = nySoeknadRequest.avdoed
                    val barnListe = nySoeknadRequest.barn
                    var soeker = nySoeknadRequest.soeker
                    if (soeker == "") {
                        soeker =
                            when (ytelse) {
                                SoeknadType.BARNEPENSJON -> barnListe.first()
                                SoeknadType.OMSTILLINGSSTOENAD -> gjenlevende
                            }
                    }
                    if (soeker == "" || barnListe.isEmpty() || avdoed == "") {
                        call.respond(HttpStatusCode.BadRequest, "Påkrevde felter mangler")
                    }
                    val request =
                        NySoeknadRequest(
                            ytelse,
                            avdoed,
                            gjenlevende,
                            barnListe,
                            soeker = soeker,
                        )

                    val brukerId = brukerTokenInfo.ident()
                    dollyService.sendSoeknadFraDolly(request, brukerId, behandlingssteg)
                    call.respond(HttpStatusCode.Created)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Noe gikk galt")
                }
            }
            post("api/v1/hent-ytelse") {
                try {
                    val request = call.receive<FoedselsnummerDTO>()
                    val fnr = haandterUgyldigIdent(request.foedselsnummer)
                    val vedtak = vedtakService.hentVedtak(fnr)
                    call.respond(vedtak)
                } catch (e: UgyldigFoedselsnummerException) {
                    call.respondNullable(HttpStatusCode.BadRequest, e.detail)
                } catch (e: IllegalArgumentException) {
                    call.respondNullable(HttpStatusCode.BadRequest, e.message)
                }
            }
        }

    fun haandterUgyldigIdent(fnr: String): Folkeregisteridentifikator {
        try {
            return Folkeregisteridentifikator.of(fnr)
        } catch (_: Exception) {
            throw UgyldigFoedselsnummerException()
        }
    }
}

data class NySoeknadRequest(
    val type: SoeknadType,
    val avdoed: String,
    val gjenlevende: String,
    val barn: List<String> = emptyList(),
    val soeker: String? = null,
)

data class SoeknadResponse(
    val status: Number,
    val noekkel: String,
)

data class VedtakTilPerson(
    val vedtak: List<Vedtak>,
)

data class Vedtak(
    val sakId: Long,
    val sakType: String,
    val virkningstidspunkt: LocalDate,
    val type: VedtakType,
    val utbetaling: List<VedtakUtbetaling>,
)

enum class VedtakType {
    INNVILGELSE,
    OPPHOER,
    AVSLAG,
    ENDRING,
}

data class VedtakUtbetaling(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val beloep: BigDecimal?,
)

class UgyldigFoedselsnummerException :
    UgyldigForespoerselException(
        code = "006-FNR-UGYLDIG",
        detail = "Ugyldig fødselsnummer",
        meta = getMeta(),
    )

fun getMeta() =
    mapOf(
        "correlation-id" to getCorrelationId(),
        "tidspunkt" to Tidspunkt.now(),
    )
