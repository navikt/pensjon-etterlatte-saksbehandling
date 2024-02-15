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
import no.nav.etterlatte.brev.dokarkiv.KnyttTilAnnenSakRequest
import no.nav.etterlatte.brev.dokarkiv.OppdaterJournalpostRequest
import no.nav.etterlatte.brev.hentinformasjon.Tilgangssjekker
import no.nav.etterlatte.libs.common.withFoedselsnummer
import no.nav.etterlatte.libs.ktor.brukerTokenInfo

fun Route.dokumentRoute(
    safService: SafService,
    dokarkivService: DokarkivService,
    tilgangssjekker: Tilgangssjekker,
) {
    route("dokumenter") {
        post {
            withFoedselsnummer(tilgangssjekker, skrivetilgang = true) { foedselsnummer ->
                val visTemaPen = call.request.queryParameters["visTemaPen"]?.toBoolean() ?: false

                val dokumenter =
                    safService.hentDokumenter(foedselsnummer.value, visTemaPen, BrukerIdType.FNR, brukerTokenInfo)

                call.respond(dokumenter)
            }
        }

        route("{journalpostId}") {
            get {
                val journalpostId = call.parameters["journalpostId"]!!

                val journalpost = safService.hentJournalpost(journalpostId, brukerTokenInfo)

                call.respond(journalpost ?: HttpStatusCode.NotFound)
            }

            put {
                val journalpostId = call.parameters["journalpostId"]!!
                val forsoekFerdistill = call.request.queryParameters["forsoekFerdigstill"].toBoolean()
                val journalfoerendeEnhet = call.request.queryParameters["journalfoerendeEnhet"]

                val request = call.receive<OppdaterJournalpostRequest>()

                val response = dokarkivService.oppdater(journalpostId, forsoekFerdistill, journalfoerendeEnhet, request)

                call.respond(response)
            }

            put("/knyttTilAnnenSak") {
                val journalpostId = call.parameters["journalpostId"]!!
                val request = call.receive<KnyttTilAnnenSakRequest>()

                call.respond(dokarkivService.knyttTilAnnenSak(journalpostId, request))
            }

            put("/feilregistrerSakstilknytning") {
                val journalpostId = call.parameters["journalpostId"]!!

                dokarkivService.feilregistrerSakstilknytning(journalpostId)
                call.respond(HttpStatusCode.OK)
            }

            put("/opphevFeilregistrertSakstilknytning") {
                val journalpostId = call.parameters["journalpostId"]!!

                dokarkivService.opphevFeilregistrertSakstilknytning(journalpostId)
                call.respond(HttpStatusCode.OK)
            }

            get("/{dokumentInfoId}") {
                val journalpostId = call.parameters["journalpostId"]!!
                val dokumentInfoId = call.parameters["dokumentInfoId"]!!
                val innhold = safService.hentDokumentPDF(journalpostId, dokumentInfoId, brukerTokenInfo)

                call.respond(innhold)
            }
        }
    }
}
