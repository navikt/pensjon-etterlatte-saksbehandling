package no.nav.etterlatte.vedtak

import com.typesafe.config.Config
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondNullable
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.AuthorizationPlugin
import no.nav.etterlatte.libs.common.vedtak.VedtakForPersonRequest
import no.nav.etterlatte.libs.ktor.token.Issuer

fun Route.vedtakRoute(
    vedtaksvurderingKlient: VedtaksvurderingKlient,
    config: Config,
) {
    // Tiltenkt for eksternt for etterlatte men internt i Nav. Initelt gjelder dette EESSI.
    route("api/vedtak/personident") {
        install(AuthorizationPlugin) {
            accessPolicyRolesEllerAdGrupper = setOf("vedtak-for-eksterne-i-nav")
            issuers = setOf(Issuer.AZURE.issuerName)
        }

        post {
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
