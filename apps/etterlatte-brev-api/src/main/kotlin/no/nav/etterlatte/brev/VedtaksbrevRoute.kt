package no.nav.etterlatte.brev

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.bruker
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@OptIn(ExperimentalTime::class)
fun Route.vedtaksbrevRoute(service: VedtaksbrevService, behandlingKlient: BehandlingKlient) {
    val logger = LoggerFactory.getLogger("no.nav.etterlatte.brev.VedaksbrevRoute")

    route("brev/behandling/{$BEHANDLINGSID_CALL_PARAMETER}") {
        get("vedtak") {
            withBehandlingId(behandlingKlient) { behandlingId ->
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
            withBehandlingId(behandlingKlient) { behandlingId ->
                val sakId = requireNotNull(call.parameters["sakId"]).toLong()

                logger.info("Oppretter vedtaksbrev for behandling (sakId=$sakId, behandlingId=$behandlingId)")

                measureTimedValue {
                    service.opprettVedtaksbrev(sakId, behandlingId, bruker)
                }.let { (brev, varighet) ->
                    logger.info("Oppretting av brev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(HttpStatusCode.Created, brev)
                }
            }
        }

        get("vedtak/pdf") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                val sakId = requireNotNull(call.parameters["sakId"]).toLong()

                logger.info("Genererer vedtaksbrev PDF (sakId=$sakId, behandlingId=$behandlingId)")

                measureTimedValue {
                    service.genererPdf(sakId, behandlingId, bruker).bytes
                }.let { (pdf, varighet) ->
                    logger.info("Oppretting av innhold/pdf tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(pdf)
                }
            }
        }

        get("vedtak/manuell") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                val sakId = requireNotNull(call.parameters["sakId"]).toLong()

                call.respond(service.hentManueltBrevPayload(behandlingId, sakId, bruker))
            }
        }

        post("vedtak/manuell") {
            withBehandlingId(behandlingKlient) {
                val body = call.receive<OppdaterPayloadRequest>()

                service.lagreManueltBrevPayload(body.id, body.payload)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

data class OppdaterPayloadRequest(
    val id: BrevID,
    val payload: Slate
)