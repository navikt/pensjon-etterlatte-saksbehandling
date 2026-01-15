package no.nav.etterlatte.tilbakekreving

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.tilbakekreving.HentOmgjoeringKravgrunnlagRequest
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.medBody
import no.nav.etterlatte.libs.ktor.route.sakId
import org.slf4j.LoggerFactory

fun Route.tilbakekrevingRoutes(tilbakekrevingService: TilbakekrevingService) {
    val logger = LoggerFactory.getLogger("TilbakekrevingRoute")

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
                val kravgrunnlagId =
                    krevIkkeNull(call.parameters["kravgrunnlagId"]?.toLong()) { "KravgrunnlagId mangler" }

                logger.info("Henter oppdatert kravgrunnlag for sak $sakId med kravgrunnlagId $kravgrunnlagId")

                val oppdatertKravgrunnlag =
                    tilbakekrevingService.hentKravgrunnlag(
                        kravgrunnlagId = kravgrunnlagId,
                        sakId = sakId,
                    )
                when (oppdatertKravgrunnlag) {
                    null -> call.respond(HttpStatusCode.NoContent)
                    else -> call.respond(oppdatertKravgrunnlag)
                }
            }
        }

        post("/omgjoering") {
            kunSystembruker {
                medBody<HentOmgjoeringKravgrunnlagRequest> { request ->
                    logger.info("Henter kravgrunnlag for omgjøring av kravgrunnlag i sak $sakId med id ${request.kravgrunnlagId}")
                    val kravgrunnlagForOmgjoering =
                        tilbakekrevingService.hentKravgrunnlagOmgjoering(
                            kravgrunnlagId = request.kravgrunnlagId,
                            sakId = sakId,
                            saksbehandler = request.saksbehandler,
                            enhet = request.enhet,
                        )
                    when (kravgrunnlagForOmgjoering) {
                        null -> call.respond(HttpStatusCode.NoContent)
                        else -> call.respond(kravgrunnlagForOmgjoering)
                    }
                }
            }
        }
    }
}
