package no.nav.etterlatte.behandling.vedtaksvurdering.routes

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.vedtaksvurdering.MismatchingIdException
import no.nav.etterlatte.behandling.vedtaksvurdering.VedtakKlageService
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang
import org.slf4j.LoggerFactory

fun Route.klagevedtakRoute(vedtakKlageService: VedtakKlageService) {
    val logger = LoggerFactory.getLogger("KlagevedtakRoute")

    route("/vedtak/klage/{$BEHANDLINGID_CALL_PARAMETER}") {
        post("/upsert") {
            kunSkrivetilgang {
                val klage = call.receive<Klage>()
                if (klage.id != behandlingId) throw MismatchingIdException("Klage-ID i path og i request body er ikke like")
                logger.info("Oppretter vedtak for klage med id=$behandlingId")

                call.respond(vedtakKlageService.opprettEllerOppdaterVedtakOmAvvisning(klage).toDto())
            }
        }

        post("/fatt") {
            kunSkrivetilgang {
                val klage = call.receive<Klage>()
                if (klage.id != behandlingId) throw MismatchingIdException("Klage-ID i path og i request body er ikke like")

                logger.info("Fatter vedtak for klage med id=$behandlingId")
                call.respond(vedtakKlageService.fattVedtak(klage, brukerTokenInfo).toDto())
            }
        }

        post("/attester") {
            kunSkrivetilgang {
                val klage = call.receive<Klage>()
                if (klage.id != behandlingId) throw MismatchingIdException("Klage-ID i path og i request body er ikke like")

                logger.info("Attesterer vedtak for klage med id=$behandlingId")
                call.respond(vedtakKlageService.attesterVedtak(klage, brukerTokenInfo).toDto())
            }
        }
        post("/underkjenn") {
            kunSkrivetilgang {
                logger.info("Underkjenner vedtak for klage=$behandlingId")
                call.respond(vedtakKlageService.underkjennVedtak(behandlingId).toDto())
            }
        }
    }
}
