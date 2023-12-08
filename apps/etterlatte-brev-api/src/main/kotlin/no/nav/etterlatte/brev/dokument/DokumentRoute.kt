package no.nav.etterlatte.brev.dokument

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.brev.dokarkiv.BrukerIdType
import no.nav.etterlatte.brev.dokarkiv.DokarkivService
import no.nav.etterlatte.brev.dokarkiv.OppdaterJournalpostRequest
import no.nav.etterlatte.brev.hentinformasjon.Tilgangssjekker
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.withFoedselsnummer
import no.nav.etterlatte.libs.ktor.brukerTokenInfo

fun Route.dokumentRoute(
    safService: SafService,
    dokarkivService: DokarkivService,
    tilgangssjekker: Tilgangssjekker,
) {
    route("dokumenter") {
        post {
            withFoedselsnummer(tilgangssjekker) { foedselsnummer ->
                val result = safService.hentDokumenter(foedselsnummer.value, BrukerIdType.FNR, brukerTokenInfo)

                if (result.error == null) {
                    call.respond(result.journalposter)
                } else {
                    call.respond(result.error.statusCode, result.error.message)
                }
            }
        }

        route("{journalpostId}") {
            get {
                val journalpostId = call.parameters["journalpostId"]!!
                val result = safService.hentJournalpost(journalpostId, brukerTokenInfo)

                call.respond(result.journalpost ?: HttpStatusCode.NotFound)
            }

            put {
                val journalpostId = call.parameters["journalpostId"]!!
                val forsoekFerdistill = call.request.queryParameters["forsoekFerdigstill"]?.toBoolean() ?: false
                val journalfoerendeEnhet = call.request.queryParameters["journalfoerendeEnhet"]

                val request = call.receive<OppdaterJournalpostRequest>()

                val response = dokarkivService.oppdater(journalpostId, forsoekFerdistill, journalfoerendeEnhet, request)

                call.respond(response)
            }

            post("/ferdigstill") {
                val sak = call.receive<Sak>()
                val journalpostId =
                    requireNotNull(call.parameters["journalpostId"]) {
                        "JournalpostID er påkrevd for å kunne ferdigstille journalposten"
                    }

                dokarkivService.ferdigstill(journalpostId, sak)
                call.respond(HttpStatusCode.OK)
            }

            put("/tema/{nyttTema}") {
                val journalpostId = call.parameters["journalpostId"]!!
                val nyttTema = call.parameters["nyttTema"]!!

                dokarkivService.endreTema(journalpostId, nyttTema)
                call.respond(HttpStatusCode.OK)
            }

            get("/{dokumentInfoId}") {
                val journalpostId = call.parameters["journalpostId"]!!
                val dokumentInfoId = call.parameters["dokumentInfoId"]!!
                val innhold = safService.hentDokumentPDF(journalpostId, dokumentInfoId, brukerTokenInfo.accessToken())

                call.respond(innhold)
            }
        }
    }
}
