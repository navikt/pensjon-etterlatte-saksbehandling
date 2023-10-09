package no.nav.etterlatte.brev

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.brev.hentinformasjon.Tilgangssjekker
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.withSakId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

const val BREV_ID_CALL_PARAMETER = "id"
inline val PipelineContext<*, ApplicationCall>.brevId: Long
    get() =
        call.parameters[BREV_ID_CALL_PARAMETER]?.toLong() ?: throw NullPointerException(
            "Brev id er ikke i path params",
        )

@OptIn(ExperimentalTime::class)
fun Route.brevRoute(
    service: BrevService,
    tilgangssjekker: Tilgangssjekker,
) {
    val logger = LoggerFactory.getLogger("no.nav.etterlatte.brev.BrevRoute")

    route("brev/{$BREV_ID_CALL_PARAMETER}") {
        get {
            withSakId(tilgangssjekker) {
                call.respond(service.hentBrev(brevId))
            }
        }

        get("pdf") {
            withSakId(tilgangssjekker) {
                val brevId = brevId

                logger.info("Genererer PDF for brev (id=$brevId)")

                measureTimedValue {
                    service.genererPdf(brevId, brukerTokenInfo).bytes
                }.let { (pdf, varighet) ->
                    logger.info("Oppretting av pdf tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(pdf)
                }
            }
        }

        post("mottaker") {
            withSakId(tilgangssjekker) {
                val body = call.receive<OppdaterMottakerRequest>()

                val mottaker = service.oppdaterMottaker(brevId, body.mottaker)

                call.respond(mottaker)
            }
        }

        route("payload") {
            get {
                withSakId(tilgangssjekker) {
                    call.respond(service.hentBrevPayload(brevId))
                }
            }

            post {
                withSakId(tilgangssjekker) {
                    val brevId = brevId
                    val body = call.receive<OppdaterPayloadRequest>()

                    service.lagreBrevPayload(brevId, body.payload)
                    body.payload_vedlegg?.let { service.lagreBrevPayloadVedlegg(brevId, it) }
                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        post("ferdigstill") {
            withSakId(tilgangssjekker) {
                service.ferdigstill(brevId, brukerTokenInfo)

                call.respond(HttpStatusCode.OK)
            }
        }

        post("journalfoer") {
            withSakId(tilgangssjekker) {
                val journalpostId = service.journalfoer(brevId, brukerTokenInfo)

                call.respond(JournalpostIdDto(journalpostId))
            }
        }

        post("distribuer") {
            withSakId(tilgangssjekker) {
                val distribusjonskvittering = service.distribuer(brevId)

                call.respond(DistribusjonskvitteringDto(distribusjonskvittering))
            }
        }
    }

    route("brev/sak/{$SAKID_CALL_PARAMETER}") {
        get {
            withSakId(tilgangssjekker) { sakId ->
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
            withSakId(tilgangssjekker) { sakId ->
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
    val payload: Slate,
    val payload_vedlegg: List<BrevInnholdVedlegg>? = null,
)

data class OppdaterMottakerRequest(
    val mottaker: Mottaker,
)

data class JournalpostIdDto(
    val journalpostId: String,
)

data class DistribusjonskvitteringDto(
    val distribusjonskvittering: String,
)
