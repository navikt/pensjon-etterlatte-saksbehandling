package no.nav.etterlatte

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.authorization
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import journalpost.JournalpostService
import no.nav.etterlatte.libs.common.brev.model.Mottaker
import no.nav.etterlatte.libs.common.journalpost.AvsenderMottaker
import no.nav.etterlatte.libs.common.journalpost.BrukerIdType
import org.slf4j.LoggerFactory

fun Route.brevRoute(service: BrevService, journalpostService: JournalpostService) {
    val logger = LoggerFactory.getLogger(BrevService::class.java)

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
            val mottakere = listOf(
                AvsenderMottaker("974761319", idType = "ORGNR", navn = "Statsforvalteren i Oslo og Viken"),
                AvsenderMottaker("974762501", idType = "ORGNR", navn = "Statsforvalteren i Vestfold og Telemark"),
                AvsenderMottaker("11057523044", idType = "FNR", navn = "Stor Snerk"),
                AvsenderMottaker("24116324268", idType = "FNR", navn = "Nobel Tøffeldyr")
            )

            call.respond(mottakere)
        }

        get("behandling/{behandlingId}") {
            val behandlingId = call.parameters["behandlingId"]!!

            call.respond(service.hentAlleBrev(behandlingId))
        }

        post("behandling/{behandlingId}") {
            val behandlingId = call.parameters["behandlingId"]!!
            val request = call.receive<OpprettBrevRequest>()

            val brevInnhold = service.opprett(request.mottaker, request.mal)
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

            val brev = service.opprett(request.mottaker, request.mal)

            call.respond(brev.data)
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

        get("innkommende/{fnr}") {
            logger.info(call.request.authorization())

            val accessToken = try {
                getAccessToken(call)
            } catch (ex: Exception) {
                logger.error("Bearer not found", ex)
                throw ex
            }
            val fnr = call.parameters["fnr"]!!

            val innhold = journalpostService.hentInnkommendeBrev(fnr, BrukerIdType.FNR, accessToken)

            call.respond(innhold)
        }

        post("innkommende/{journalpostId}/{dokumentInfoId}") {
            val accessToken = try {
                getAccessToken(call)
            } catch (ex: Exception) {
                logger.error("Bearer not found", ex)
                throw ex
            }

            val journalpostId = call.parameters["journalpostId"]!!
            val dokumentInfoId = call.parameters["dokumentInfoId"]!!
            val innhold = journalpostService.hentInnkommendeBrevInnhold(journalpostId, dokumentInfoId, accessToken)

            call.respond(innhold)
        }
    }
}

data class Mal(
    val tittel: String,
    val navn: String
)

data class OpprettBrevRequest(
    val mal: Mal,
    val mottaker: Mottaker
)