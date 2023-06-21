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
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.withSakId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@OptIn(ExperimentalTime::class)
fun Route.brevRoute(service: BrevService, behandlingKlient: BehandlingKlient) {
    val logger = LoggerFactory.getLogger("no.nav.etterlatte.brev.VedaksbrevRoute")

    route("brev/{id}") {
        get {
            withSakId(behandlingKlient) {
                val id = requireNotNull(call.parameters["id"]).toLong()

                call.respond(service.hentBrev(id))
            }
        }

        get("pdf") {
            withSakId(behandlingKlient) {
                val brevId = requireNotNull(call.parameters["id"]).toLong()

                logger.info("Genererer PDF for brev (id=$brevId)")

                measureTimedValue {
                    service.genererPdf(brevId, brukerTokenInfo).bytes
                }.let { (pdf, varighet) ->
                    logger.info("Oppretting av pdf tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(pdf)
                }
            }
        }

        route("payload") {
            get {
                withSakId(behandlingKlient) {
                    val brevId = requireNotNull(call.parameters["id"]).toLong()

                    call.respond(service.hentBrevPayload(brevId) ?: HttpStatusCode.NoContent)
                }
            }

            post {
                withSakId(behandlingKlient) {
                    val brevId = requireNotNull(call.parameters["id"]).toLong()
                    val body = call.receive<OppdaterPayloadRequest>()

                    service.lagreBrevPayload(brevId, body.payload)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        post("ferdigstill") {
            withSakId(behandlingKlient) {
                val brevId = requireNotNull(call.parameters["id"]).toLong()

                service.ferdigstill(brevId, brukerTokenInfo)

                call.respond(HttpStatusCode.OK)
            }
        }

        post("journalfoer") {
            withSakId(behandlingKlient) {
                val brevId = requireNotNull(call.parameters["id"]).toLong()

                val journalpostId = service.journalfoer(brevId, brukerTokenInfo)

                call.respond(journalpostId)
            }
        }

        post("distribuer") {
            withSakId(behandlingKlient) {
                val brevId = requireNotNull(call.parameters["id"]).toLong()

                val journalpostId = service.distribuer(brevId)

                call.respond(journalpostId)
            }
        }
    }

    route("brev/sak/{$SAKID_CALL_PARAMETER}") {
        get {
            withSakId(behandlingKlient) { sakId ->
                logger.info("Henter brev tilknyttet sak=$sakId")

                measureTimedValue {
                    service.hentBrevForSak(sakId)
                }.let { (brev, varighet) ->
                    logger.info("Henting av brev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(brev)
                }
            }
        }

        post {
            withSakId(behandlingKlient) { sakId ->
                logger.info("Oppretter nytt brev på sak=$sakId)")

                measureTimedValue {
                    service.opprettBrev(sakId, brukerTokenInfo)
                }.let { (brev, varighet) ->
                    logger.info("Oppretting av brev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(HttpStatusCode.Created, brev)
                }
            }
        }
    }
}

data class OppdaterPayloadRequest(
    val payload: Slate
)