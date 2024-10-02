package no.nav.etterlatte.brev

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.readAllParts
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.brev.distribusjon.Brevdistribuerer
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.hentinformasjon.grunnlag.GrunnlagService
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettJournalfoerOgDistribuerRequest
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.pdf.PDFService
import no.nav.etterlatte.libs.common.brev.BestillingsIdDto
import no.nav.etterlatte.libs.common.brev.JournalpostIdDto
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.withSakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

const val BREV_ID_CALL_PARAMETER = "id"
inline val PipelineContext<*, ApplicationCall>.brevId: Long
    get() =
        call.parameters[BREV_ID_CALL_PARAMETER]?.toLong() ?: throw NullPointerException(
            "Brev id er ikke i path params",
        )

fun Route.brevRoute(
    service: BrevService,
    pdfService: PDFService,
    distribuerer: Brevdistribuerer,
    tilgangssjekker: Tilgangssjekker,
    grunnlagService: GrunnlagService,
    behandlingService: BehandlingService,
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
            withSakId(tilgangssjekker, skrivetilgang = true) {
                val body = call.receive<OppdaterMottakerRequest>()

                val mottaker = service.oppdaterMottaker(brevId, body.mottaker)

                call.respond(mottaker)
            }
        }

        post("tittel") {
            withSakId(tilgangssjekker, skrivetilgang = true) {
                val request = call.receive<OppdaterTittelRequest>()

                service.oppdaterTittel(brevId, request.tittel)

                call.respond(HttpStatusCode.OK)
            }
        }

        post("spraak") {
            withSakId(tilgangssjekker, skrivetilgang = true) {
                val request = call.receive<OppdaterSpraakRequest>()

                service.oppdaterSpraak(brevId, request.spraak)

                call.respond(HttpStatusCode.OK)
            }
        }

        route("payload") {
            get {
                withSakId(tilgangssjekker) {
                    call.respond(service.hentBrevPayload(brevId))
                }
            }

            post {
                withSakId(tilgangssjekker, skrivetilgang = true) {
                    val brevId = brevId
                    val body = call.receive<OppdaterPayloadRequest>()

                    service.lagreBrevPayload(brevId, body.payload)
                    body.payload_vedlegg?.let { service.lagreBrevPayloadVedlegg(brevId, it) }
                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        post("ferdigstill") {
            withSakId(tilgangssjekker, skrivetilgang = true) {
                service.ferdigstill(brevId, brukerTokenInfo)

                call.respond(HttpStatusCode.OK)
            }
        }

        post("journalfoer") {
            withSakId(tilgangssjekker, skrivetilgang = true) {
                val journalpostId = service.journalfoer(brevId, brukerTokenInfo)

                call.respond(JournalpostIdDto(journalpostId))
            }
        }

        post("distribuer") {
            withSakId(tilgangssjekker, skrivetilgang = true) {
                val queryparamDistribusjonstype = call.request.queryParameters["distribusjonsType"]
                val distribusjonsType =
                    when (queryparamDistribusjonstype) {
                        is String -> DistribusjonsType.valueOf(queryparamDistribusjonstype)
                        else -> DistribusjonsType.ANNET
                    }
                val journalpostIdInn = call.request.queryParameters["journalpostIdInn"]
                val bestillingsId =
                    distribuerer.distribuer(
                        brevId,
                        distribusjonsType = distribusjonsType,
                        journalpostIdInn = journalpostIdInn,
                    )

                call.respond(BestillingsIdDto(bestillingsId))
            }
        }

        delete {
            withSakId(tilgangssjekker, skrivetilgang = true) {
                call.respond(service.slett(brevId, brukerTokenInfo))
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
            // TODO: sjekk om unused, erstattet med /spesifikk?
            withSakId(tilgangssjekker, skrivetilgang = true) { sakId ->
                logger.info("Oppretter nytt brev på sak=$sakId)")

                measureTimedValue {
                    service.opprettNyttManueltBrev(
                        sakId,
                        brukerTokenInfo,
                        Brevkoder.TOMT_INFORMASJONSBREV,
                        ManueltBrevData(),
                    )
                }.let { (brev, varighet) ->
                    logger.info("Oppretting av brev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(HttpStatusCode.Created, brev)
                }
            }
        }

        post("spesifikk") {
            withSakId(tilgangssjekker, skrivetilgang = true) { sakId ->
                logger.info("Oppretter nytt brev på sak=$sakId)")
                val brevParametre = call.receive<BrevParametre>()
                val sak = behandlingService.hentSak(sakId, brukerTokenInfo)
                grunnlagService.oppdaterGrunnlagForSak(sak, brukerTokenInfo)
                measureTimedValue {
                    service.opprettNyttManueltBrev(
                        sakId,
                        brukerTokenInfo,
                        brevParametre.brevkode,
                        brevParametre.brevDataMapping(),
                    )
                }.let { (brev, varighet) ->
                    logger.info("Oppretting av brev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(HttpStatusCode.Created, brev)
                }
            }
        }

        post("opprett-journalfoer-og-distribuer") {
            kunSystembruker {
                withSakId(tilgangssjekker, skrivetilgang = true) {
                    val req = call.receive<OpprettJournalfoerOgDistribuerRequest>()

                    service.opprettJournalfoerOgDistribuerRiver(brukerTokenInfo, req)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        post("pdf") {
            withSakId(tilgangssjekker, skrivetilgang = true) { sakId ->
                try {
                    val brev = pdfService.lagreOpplastaPDF(sakId, call.receiveMultipart().readAllParts())
                    brev.onSuccess {
                        call.respond(brev)
                    }
                    brev.onFailure {
                        call.respond(HttpStatusCode.UnprocessableEntity)
                    }
                } catch (e: Exception) {
                    logger.error("Getting multipart error", e)
                    call.respond(HttpStatusCode.BadRequest)
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

data class OppdaterTittelRequest(
    val tittel: String,
)

data class OppdaterSpraakRequest(
    val spraak: Spraak,
)
