package no.nav.etterlatte.brev.dokument

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.brev.journalpost.BrukerIdType
import no.nav.etterlatte.libs.common.withFoedselsnummer
import no.nav.etterlatte.libs.ktor.brukerTokenInfo

fun Route.dokumentRoute(safService: SafService, behandlingKlient: BehandlingKlient) {
    route("dokumenter") {
        post {
            withFoedselsnummer(behandlingKlient) { foedselsnummer ->
                val result = safService.hentDokumenter(foedselsnummer.value, BrukerIdType.FNR, brukerTokenInfo)

                if (result.error == null) {
                    call.respond(result.journalposter)
                } else {
                    call.respond(result.error.statusCode, result.error.message)
                }
            }
        }

        get("{journalpostId}/{dokumentInfoId}") {
            val journalpostId = call.parameters["journalpostId"]!!
            val dokumentInfoId = call.parameters["dokumentInfoId"]!!
            val innhold = safService.hentDokumentPDF(journalpostId, dokumentInfoId, brukerTokenInfo.accessToken())

            call.respond(innhold)
        }
    }
}