package no.nav.etterlatte.brev

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.LoggerFactory

fun Route.brevRoute(service: BrevService) {
    val logger = LoggerFactory.getLogger("BrevRoute")

    route("api/behandling/brev/{$BEHANDLINGID_CALL_PARAMETER}/vedtak") {
        post {
            logger.info("Oppretter vedtaksbrev for tilbakekreving behandling (sakId=$sakId, behandlingId=$behandlingId)")
            val brev = service.opprettVedtaksbrev(behandlingId, sakId, brukerTokenInfo)
            call.respond(HttpStatusCode.Created, brev)
        }

        get("pdf") {
            val brevId =
                krevIkkeNull(call.request.queryParameters["brevId"]?.toLong()) {
                    "Kan ikke generere PDF uten brevId"
                }
            logger.info("Genererer PDF for tilbakekreving vedtaksbrev (id=$brevId)")

            val pdf = service.genererPdf(brevId, behandlingId, sakId, brukerTokenInfo).bytes
            call.respond(pdf)
        }
    }
}
