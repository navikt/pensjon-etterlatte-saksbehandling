package no.nav.etterlatte.behandling.vedtaksvurdering.routes

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.vedtaksvurdering.VedtakEtteroppgjoerService
import no.nav.etterlatte.libs.common.vedtak.VedtakslisteEtteroppgjoerRequest
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang

fun Route.etteroppgjoerSystembrukerVedtakRoute(vedtakEtteroppgjoerService: VedtakEtteroppgjoerService) {
    route("/vedtak/etteroppgjoer/{$SAKID_CALL_PARAMETER}") {
        post {
            kunSkrivetilgang {
                val request = call.receive<VedtakslisteEtteroppgjoerRequest>()

                val vedtaksliste =
                    vedtakEtteroppgjoerService.hentVedtakslisteIEtteroppgjoersAar(
                        sakId = request.sakId,
                        etteroppgjoersAar = request.etteroppgjoersAar,
                    )
                call.respond(vedtaksliste)
            }
        }
    }
}
