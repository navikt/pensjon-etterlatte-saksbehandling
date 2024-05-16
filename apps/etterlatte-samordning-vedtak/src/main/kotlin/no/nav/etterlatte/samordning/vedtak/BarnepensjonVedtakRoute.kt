package no.nav.etterlatte.samordning.vedtak

import com.typesafe.config.Config
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.response.respondNullable
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktor.AuthorizationPlugin
import no.nav.etterlatte.libs.ktor.route.dato

fun Route.barnepensjonVedtakRoute(
    samordningVedtakService: SamordningVedtakService,
    config: Config,
) {
    route("api/pensjon/barnepensjon/vedtak") {
        install(AuthorizationPlugin) {
            roles = setOf("les-bp-vedtak", config.getString("roller.pensjon-saksbehandler"))
            issuers = setOf("azure")
        }

        get {
            val fomDato = call.dato("fomDato") ?: throw ManglerFomDatoException()
            val tomDato = call.dato("tomDato")
            val fnr = call.fnr

            val vedtakliste =
                try {
                    samordningVedtakService.hentVedtaksliste(
                        fomDato = fomDato,
                        tomDato = tomDato,
                        fnr = Folkeregisteridentifikator.of(fnr),
                        sakType = SakType.BARNEPENSJON,
                        context = PensjonContext,
                    )
                } catch (e: IllegalArgumentException) {
                    return@get call.respondNullable(HttpStatusCode.BadRequest, e.message)
                }

            call.respond(vedtakliste)
        }

        get("/ping") {
            call.respond(
                getMeta(),
            )
        }
    }
}
