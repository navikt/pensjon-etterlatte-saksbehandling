package no.nav.etterlatte.dokument

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.brev.getAccessToken
import no.nav.etterlatte.libs.common.journalpost.BrukerIdType
import org.slf4j.LoggerFactory

fun Route.dokumentRoute(journalpostService: JournalpostService) {
    val logger = LoggerFactory.getLogger("no.nav.etterlatte.dokument.DokumentRoute")

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

        get("{journalpostId}/{dokumentInfoId}") {
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
}
