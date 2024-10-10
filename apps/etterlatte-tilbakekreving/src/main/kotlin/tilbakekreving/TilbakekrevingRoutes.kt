package no.nav.etterlatte.tilbakekreving

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.routeLogger

fun Route.tilbakekrevingRoutes(tilbakekrevingService: TilbakekrevingService) {
    val logger = routeLogger

    route("/api/tilbakekreving/{sakId}") {
        post("/vedtak") {
            kunSystembruker {
                val vedtak = call.receive<TilbakekrevingVedtak>()

                logger.info("Sender tilbakekrevingsvedtak for sak ${vedtak.sakId}")

                tilbakekrevingService.sendTilbakekrevingsvedtak(vedtak)
                call.respond(HttpStatusCode.Created)
            }
        }

        get("/kravgrunnlag/{kravgrunnlagId}") {
            kunSystembruker {
                val sakId = SakId(requireNotNull(call.parameters["sakId"]).toLong())
                val kravgrunnlagId = requireNotNull(call.parameters["kravgrunnlagId"]).toLong()

                logger.info("Henter oppdatert kravgrunnlag for sak $sakId med kravgrunnlagId $kravgrunnlagId")

                val oppdatertKravgrunnlag =
                    tilbakekrevingService.hentKravgrunnlag(
                        kravgrunnlagId = kravgrunnlagId,
                        sakId = sakId,
                    )
                call.respond(oppdatertKravgrunnlag)
            }
        }
    }
}
