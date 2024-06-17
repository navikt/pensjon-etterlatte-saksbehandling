package no.nav.etterlatte.tilbakekreving.vedtak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.routeLogger

fun Route.tilbakekrevingRoutes(tilbakekrevingVedtakService: TilbakekrevingService) {
    val logger = routeLogger

    route("/api/tilbakekreving/{sakId}") {
        post("/vedtak") {
            kunSystembruker {
                logger.info("Sender tilbakekrevingsvedtak")
                val vedtak = call.receive<TilbakekrevingVedtak>()
                tilbakekrevingVedtakService.sendTilbakekrevingsvedtak(vedtak)
                call.respond(HttpStatusCode.Created)
            }
        }

        get("/kravgrunnlag/{kravgrunnlagId}") {
            kunSystembruker {
                logger.info("Henter oppdatert kravgrunnlag")
                val sakId = requireNotNull(call.parameters["sakId"]).toLong()
                val kravgrunnlagId = requireNotNull(call.parameters["kravgrunnlagId"]).toLong()

                val oppdatertKravgrunnlag =
                    tilbakekrevingVedtakService.hentKravgrunnlag(
                        kravgrunnlagId = kravgrunnlagId,
                        sakId = sakId,
                    )
                call.respond(oppdatertKravgrunnlag)
            }
        }
    }
}
