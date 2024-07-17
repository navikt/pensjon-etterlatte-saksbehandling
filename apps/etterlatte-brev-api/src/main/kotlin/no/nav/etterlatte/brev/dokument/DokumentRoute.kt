package no.nav.etterlatte.brev.dokument

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.brev.dokarkiv.DokarkivService
import no.nav.etterlatte.brev.dokarkiv.KnyttTilAnnenSakRequest
import no.nav.etterlatte.brev.dokarkiv.OppdaterJournalpostRequest
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo

fun Route.dokumentRoute(
    safService: SafService,
    dokarkivService: DokarkivService,
    tilgangssjekker: Tilgangssjekker,
) {
    route("dokumenter") {
        post {
            val request = call.receive<HentDokumenterRequest>()

            val harTilgang =
                tilgangssjekker.harTilgangTilPerson(
                    foedselsnummer = request.foedselsnummer,
                    skrivetilgang = true,
                    bruker = brukerTokenInfo as Saksbehandler,
                )

            if (harTilgang) {
                val dokumenter = safService.hentDokumenter(request, brukerTokenInfo)

                call.respond(dokumenter)
            } else {
                call.respond(HttpStatusCode.Forbidden)
            }
        }

        route("{journalpostId}") {
            get {
                val journalpost = safService.hentJournalpost(journalpostId, brukerTokenInfo)

                call.respond(journalpost)
            }

            put {
                val forsoekFerdigstill = call.request.queryParameters["forsoekFerdigstill"].toBoolean()
                val journalfoerendeEnhet = call.request.queryParameters["journalfoerendeEnhet"]

                val request = call.receive<OppdaterJournalpostRequest>()

                val response = dokarkivService.oppdater(journalpostId, forsoekFerdigstill, journalfoerendeEnhet, request)

                call.respond(response)
            }

            put("/knyttTilAnnenSak") {
                val request = call.receive<KnyttTilAnnenSakRequest>()

                call.respond(dokarkivService.knyttTilAnnenSak(journalpostId, request))
            }

            put("/feilregistrerSakstilknytning") {
                dokarkivService.feilregistrerSakstilknytning(journalpostId)
                call.respond(HttpStatusCode.OK)
            }

            put("/opphevFeilregistrertSakstilknytning") {
                dokarkivService.opphevFeilregistrertSakstilknytning(journalpostId)
                call.respond(HttpStatusCode.OK)
            }

            get("/utsendingsinfo") {
                val utsendingsinfo = safService.hentUtsendingsinfo(journalpostId, brukerTokenInfo)
                call.respond(utsendingsinfo ?: HttpStatusCode.NoContent)
            }

            get("/{dokumentInfoId}") {
                val dokumentInfoId = call.parameters["dokumentInfoId"]!!
                val innhold = safService.hentDokumentPDF(journalpostId, dokumentInfoId, brukerTokenInfo)

                call.respond(innhold)
            }
        }
    }
}

private inline val PipelineContext<*, ApplicationCall>.journalpostId: String
    get() =
        requireNotNull(call.parameters["journalpostId"]) {
            "JournalpostID mangler i requesten"
        }

data class HentDokumenterRequest(
    val foedselsnummer: Folkeregisteridentifikator,
    val journalstatuser: List<Journalstatus> = emptyList(),
    val journalposttyper: List<Journalposttype> = emptyList(),
    val tema: List<String> = emptyList(),
    val foerste: Int = 50,
)
