package no.nav.etterlatte.brev

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.brev.hentinformasjon.Tilgangssjekker
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@OptIn(ExperimentalTime::class)
fun Route.vedtaksbrevRoute(
    service: VedtaksbrevService,
    tilgangssjekker: Tilgangssjekker,
) {
    val logger = LoggerFactory.getLogger("no.nav.etterlatte.brev.VedaksbrevRoute")

    route("brev/behandling/{$BEHANDLINGSID_CALL_PARAMETER}") {
        get("vedtak") {
            withBehandlingId(tilgangssjekker) { behandlingId ->
                logger.info("Henter vedtaksbrev for behandling (behandlingId=$behandlingId)")

                measureTimedValue {
                    service.hentVedtaksbrev(behandlingId)
                }.let { (brev, varighet) ->
                    logger.info("Henting av brev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(brev ?: HttpStatusCode.NoContent)
                }
            }
        }

        post("vedtak") {
            withBehandlingId(tilgangssjekker) { behandlingId ->
                val sakId = sakId

                logger.info("Oppretter vedtaksbrev for behandling (sakId=$sakId, behandlingId=$behandlingId)")

                measureTimedValue {
                    service.opprettVedtaksbrev(sakId, behandlingId, brukerTokenInfo)
                }.let { (brev, varighet) ->
                    logger.info("Oppretting av brev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(HttpStatusCode.Created, brev)
                }
            }
        }

        get("vedtak/pdf") {
            withBehandlingId(tilgangssjekker) {
                val brevId = requireNotNull(call.parameters["brevId"]).toLong()

                logger.info("Genererer PDF for vedtaksbrev (id=$brevId)")

                measureTimedValue {
                    service.genererPdf(brevId, brukerTokenInfo).bytes
                }.let { (pdf, varighet) ->
                    logger.info("Generering av pdf tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(pdf)
                }
            }
        }

        post("vedtak/ferdigstill") {
            withBehandlingId(tilgangssjekker) { behandlingId ->
                logger.info("Ferdigstiller vedtaksbrev for behandling (id=$behandlingId)")

                measureTimedValue {
                    service.ferdigstillVedtaksbrev(behandlingId, brukerTokenInfo)
                }.also { (_, varighet) ->
                    logger.info("Ferdigstilling av vedtaksbrev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        put("payload/tilbakestill") {
            withBehandlingId(tilgangssjekker) {
                val body = call.receive<ResetPayloadRequest>()
                val brevId = body.brevId
                val sakId = body.sakId

                logger.info("Tilbakestiller payload for vedtaksbrev (id=$brevId)")

                measureTimedValue {
                    service.hentNyttInnhold(sakId, brevId, behandlingsId, brukerTokenInfo)
                }.let { (brevPayload, varighet) ->
                    logger.info(
                        "Oppretting av nytt innhold til brev (id=$brevId) tok ${varighet.toString(
                            DurationUnit.SECONDS,
                            2,
                        )}",
                    )
                    call.respond(brevPayload)
                }
            }
        }
    }
}

data class ResetPayloadRequest(
    val brevId: Long,
    val sakId: Long,
)
