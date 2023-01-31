package no.nav.etterlatte.brev.dokument

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.brev.getAccessToken
import no.nav.etterlatte.libs.common.journalpost.BrukerIdType
import org.slf4j.LoggerFactory

fun Route.dokumentRoute(safService: SafService) {
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
            val innhold = safService.hentDokumenter(fnr, BrukerIdType.FNR, accessToken)

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
            val innhold = safService.hentDokumentPDF(journalpostId, dokumentInfoId, accessToken)

            call.respond(innhold)
        }
    }
}