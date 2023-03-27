package no.nav.etterlatte.brev.dokument

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.brev.journalpost.BrukerIdType
import no.nav.etterlatte.brev.tilgangssjekk.BehandlingKlient
import no.nav.etterlatte.libs.common.FoedselsnummerDTO
import no.nav.etterlatte.libs.common.withFoedselsnummer
import no.nav.etterlatte.libs.ktor.bruker

fun Route.dokumentRoute(safService: SafService, behandlingKlient: BehandlingKlient) {
    route("dokumenter") {
        post {
            val foedselsnummerDTO = call.receive<FoedselsnummerDTO>()
            val fnr = foedselsnummerDTO.foedselsnummer
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