package no.nav.etterlatte.brev.dokument

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.brev.tilgangssjekk.BehandlingKlient
import no.nav.etterlatte.libs.common.journalpost.BrukerIdType
import no.nav.etterlatte.libs.common.withFoedselsnummer
import no.nav.etterlatte.libs.ktor.bruker
import org.slf4j.LoggerFactory

fun Route.dokumentRoute(safService: SafService, behandlingKlient: BehandlingKlient) {
    val logger = LoggerFactory.getLogger("no.nav.etterlatte.dokument.DokumentRoute")

    route("dokumenter") {
        get("{fnr}") {
            val fnr = call.parameters["fnr"]!!
            withFoedselsnummer(fnr, behandlingKlient) { foedselsnummer ->
                val innhold = safService.hentDokumenter(foedselsnummer.value, BrukerIdType.FNR, bruker.accessToken())
                call.respond(innhold)
            }
        }

        get("{journalpostId}/{dokumentInfoId}") {
            val journalpostId = call.parameters["journalpostId"]!!
            val dokumentInfoId = call.parameters["dokumentInfoId"]!!
            val innhold = safService.hentDokumentPDF(journalpostId, dokumentInfoId, bruker.accessToken())

            call.respond(innhold)
        }
    }
}