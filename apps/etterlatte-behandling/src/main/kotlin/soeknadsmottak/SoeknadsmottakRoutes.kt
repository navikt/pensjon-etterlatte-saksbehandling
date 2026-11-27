package no.nav.etterlatte.soeknadsmottak

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.vedtaksvurdering.routes.withSerializableRetry
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.soeknadsmottak.JournalfoeringskontekstRequest
import no.nav.etterlatte.libs.common.soeknadsmottak.SoeknadOverleveringRequest
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.medBody
import no.nav.etterlatte.libs.ktor.token.Systembruker

/** Bare etterlatte-api får kalle mottaket. Innsendt-soeknad går via etterlatte-api med egen rolle. */
private const val TILLATT_APP = "etterlatte-api"

fun Route.soeknadsmottakRoutes(service: SoeknadsmottakService) {
    route("/api/soeknadsmottak") {
        post("/journalfoeringskontekst") {
            kunSystembruker { systembruker ->
                krevTillattApp(systembruker)
                medBody<JournalfoeringskontekstRequest> { request ->
                    call.respond(withSerializableRetry { inTransaction { service.journalfoeringskontekst(request) } })
                }
            }
        }

        post("/overlevering") {
            kunSystembruker { systembruker ->
                krevTillattApp(systembruker)
                medBody<SoeknadOverleveringRequest> { request ->
                    // SERIALIZABLE: et parallelt kall med samme nøkkel får 40001 og ser den andres resultat i ny transaksjon.
                    call.respond(withSerializableRetry { inTransaction { service.overlever(request) } })
                }
            }
        }
    }
}

private fun krevTillattApp(systembruker: Systembruker) {
    // azp_name har formatet cluster:namespace:app
    if (systembruker.ident.substringAfterLast(":") != TILLATT_APP) {
        throw IkkeTillattException("SOEKNADSMOTTAK_IKKE_TILLATT", "Søknadsmottaket kan bare kalles av $TILLATT_APP")
    }
}
