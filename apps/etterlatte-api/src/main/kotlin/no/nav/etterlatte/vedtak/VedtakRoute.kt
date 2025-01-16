package no.nav.etterlatte.vedtak

import com.typesafe.config.Config
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondNullable
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.vedtak.VedtakForPersonRequest

fun Route.vedtakRoute(
    vedtaksvurderingKlient: VedtaksvurderingKlient,
    config: Config,
) {
    // Tiltenkt for eksternt for etterlatte men internt i Nav. Initelt gjelder dette EESSI.
    route("api/vedtak/personident") {
        post {
            // TODO mer autentisering?
            try {
                val request = call.receive<VedtakForPersonRequest>()
                val vedtak = vedtaksvurderingKlient.hentVedtak(request)
                call.respond(vedtak)
            } catch (e: IllegalArgumentException) {
                call.respondNullable(HttpStatusCode.BadRequest, e.message)
            }
        }
    }
}
