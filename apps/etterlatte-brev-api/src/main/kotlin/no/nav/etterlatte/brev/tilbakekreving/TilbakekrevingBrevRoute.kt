package no.nav.etterlatte.brev.tilbakekreving

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

fun Route.tilbakekrevingBrevRoute(
    service: TilbakekrevingVedtaksbrevService,
    tilgangssjekker: Tilgangssjekker,
) {
    val logger = LoggerFactory.getLogger("no.nav.etterlatte.brev.tilbakekreving.TilbakekrevingBrevRoute")
    route("brev/tilbakekreving/{$BEHANDLINGID_CALL_PARAMETER}") {
        post("vedtak") {
            withBehandlingId(tilgangssjekker, skrivetilgang = true) { behandlingId ->
                val sakId = sakId

                logger.info("Oppretter vedtaksbrev for behandling (sakId=$sakId, behandlingId=$behandlingId)")

                measureTimedValue {
                    service.opprettVedtaksbrev(sakId, behandlingId, brukerTokenInfo)
                }.let { (brev, varighet) ->
                    logger.info("Oppretting av brev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(HttpStatusCode.Created, brev)
                }
            }
        }
    }
}
