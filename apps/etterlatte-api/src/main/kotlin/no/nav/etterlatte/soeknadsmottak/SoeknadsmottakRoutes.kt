package no.nav.etterlatte.soeknadsmottak

import io.ktor.server.application.install
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.AuthorizationPlugin
import no.nav.etterlatte.libs.common.soeknadsmottak.JournalfoeringskontekstRequest
import no.nav.etterlatte.libs.common.soeknadsmottak.SoeknadOverleveringRequest
import no.nav.etterlatte.libs.ktor.token.Issuer

/** Rollen innsendt-soeknad får i accessPolicy. Gir bare tilgang til søknadsmottaket. */
const val SOEKNADSMOTTAK_ROLLE = "soeknadsmottak"

/**
 * Grenseflaten for søknadsmottaket i innsendt-soeknad (B12). API-et videreformidler bare;
 * etterlatte-behandling avgjør sak, enhet og oppgave og eier dedupliseringen.
 */
fun Route.soeknadsmottakRoutes(klient: SoeknadsmottakKlient) {
    route("api/soeknad/mottak") {
        install(AuthorizationPlugin) {
            accessPolicyRolesEllerAdGrupper = setOf(SOEKNADSMOTTAK_ROLLE)
            issuers = setOf(Issuer.AZURE.issuerName)
        }

        post("journalfoeringskontekst") {
            call.respond(klient.journalfoeringskontekst(call.receive<JournalfoeringskontekstRequest>()))
        }

        post("overlevering") {
            call.respond(klient.overlever(call.receive<SoeknadOverleveringRequest>()))
        }
    }
}
