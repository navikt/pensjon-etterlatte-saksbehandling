package no.nav.etterlatte.brev

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.http.content.streamProvider
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.journalpost.JournalpostService
import no.nav.etterlatte.libs.common.brev.model.BrevInnhold
import no.nav.etterlatte.libs.common.brev.model.Mottaker
import no.nav.etterlatte.libs.common.journalpost.AvsenderMottaker
import no.nav.etterlatte.libs.common.journalpost.BrukerIdType
import no.nav.etterlatte.libs.common.objectMapper
import org.slf4j.LoggerFactory

fun Route.brevRoute(service: BrevService, mottakerService: MottakerService, journalpostService: JournalpostService) {
    val logger = LoggerFactory.getLogger(BrevService::class.java)

    route("dokumenter") {
        get("{fnr}") {
            val accessToken = try {
                getAccessToken(call)
            } catch (ex: Exception) {
                logger.error("Bearer not found", ex)
                throw ex
            }

            val fnr = call.parameters["fnr"]!!
            val innhold = journalpostService.hentDokumenter(fnr, BrukerIdType.FNR, accessToken)

            call.respond(innhold)
        }

        post("{journalpostId}/{dokumentInfoId}") {
            val accessToken = try {
                getAccessToken(call)
            } catch (ex: Exception) {
                logger.error("Bearer not found", ex)
                throw ex
            }

            val journalpostId = call.parameters["journalpostId"]!!
            val dokumentInfoId = call.parameters["dokumentInfoId"]!!
            val innhold = journalpostService.hentDokumentPDF(journalpostId, dokumentInfoId, accessToken)

            call.respond(innhold)
        }
    }

    route("brev") {
        get("maler") {
            val maler = listOf(
                Mal("Vedtak om innvilget barnepensjon", "innvilget"),
                Mal("Revurdert barnepensjon", "revurdering"),
                Mal("Dokumentasjon om vergemål", "verge")
            )

            call.respond(maler)
        }

        get("mottakere") {
            val statsforvaltere = mottakerService.hentStatsforvalterListe().map {
                AvsenderMottaker(it.organisasjonsnummer, idType = "ORGNR", it.navn)
            }

            // todo: Hent personer fra saksbehandlingen
            val personer = listOf(
                AvsenderMottaker("11057523044", idType = "FNR", navn = "Stor Snerk"),
                AvsenderMottaker("24116324268", idType = "FNR", navn = "Nobel Tøffeldyr")
            )

            call.respond(statsforvaltere + personer)
        }

        get("behandling/{behandlingId}") {
            val behandlingId = call.parameters["behandlingId"]!!

            call.respond(service.hentAlleBrev(behandlingId))
        }

        post("behandling/{behandlingId}") {
            val behandlingId = call.parameters["behandlingId"]!!
            val request = call.receive<OpprettBrevRequest>()

            val brevInnhold = service.opprett(request.mottaker, request.mal, request.enhet)
            val brev = service.lagreAnnetBrev(behandlingId, request.mottaker, brevInnhold)

            call.respond(brev)
        }

        post("behandling/{behandlingId}/vedtak") {
            val behandlingId = call.parameters["behandlingId"]!!

            val brev = service.oppdaterVedtaksbrev(behandlingId)

            call.respond(brev)
        }

        post("forhaandsvisning") {
            val request = call.receive<OpprettBrevRequest>()

            val brev = service.opprett(request.mottaker, request.mal, request.enhet)

            call.respond(brev.data)
        }

        post("pdf/{behandlingId}") {
            val behandlingId = call.parameters["behandlingId"]!!

            try {
                val mp = call.receiveMultipart().readAllParts()

                val filData = mp.first { it is PartData.FormItem }
                    .let { objectMapper.readValue<FilData>((it as PartData.FormItem).value) }

                val fil: ByteArray = mp.first { it is PartData.FileItem }
                    .let { (it as PartData.FileItem).streamProvider().readBytes() }

                val brevInnhold = BrevInnhold(filData.filNavn, "nb", fil)

                val brev = service.lagreAnnetBrev(behandlingId, filData.mottaker, brevInnhold)

                call.respond(brev)
            } catch (e: Exception) {
                logger.error("Getting multipart error", e)
                call.respond(HttpStatusCode.BadRequest)
            }
        }

        post("{brevId}/pdf") {
            val brevId = call.parameters["brevId"]!!
            val innhold = service.hentBrevInnhold(brevId.toLong())

            call.respond(innhold.data)
        }

        delete("{brevId}") {
            val brevId = call.parameters["brevId"]!!

            val brev = service.slettBrev(brevId.toLong())

            if (brev) {
                call.respond("OK")
            } else {
                call.respond(HttpStatusCode.BadRequest)
            }
        }

        post("{brevId}/ferdigstill") {
            val brevId = call.parameters["brevId"]!!

            val brev = service.ferdigstillBrev(brevId.toLong())

            call.respond(brev)
        }
    }
}

data class Mal(
    val tittel: String,
    val navn: String
)

data class OpprettBrevRequest(
    val mal: Mal,
    val mottaker: Mottaker,
    val enhet: String
)

data class FilData(
    val mottaker: Mottaker,
    val filNavn: String
)

private fun getAccessToken(call: ApplicationCall): String {
    val authHeader = call.request.parseAuthorizationHeader()
    if (!(authHeader == null || authHeader !is HttpAuthHeader.Single || authHeader.authScheme != "Bearer")) {
        return authHeader.blob
    }
    throw Exception("Missing authorization header")
}