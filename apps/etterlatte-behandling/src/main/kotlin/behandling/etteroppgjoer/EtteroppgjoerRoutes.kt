package no.nav.etterlatte.behandling.etteroppgjoer

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.ktor.route.ETTEROPPGJOER_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.etteroppgjoerId
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang

enum class EtteroppgjoerToggles(
    private val toggle: String,
) : FeatureToggle {
    ETTEROPPGJOER("etteroppjoer"),
    ;

    override fun key(): String = toggle
}

fun Route.etteroppgjoerRoutes(
    service: EtteroppgjoerService,
    featureToggleService: FeatureToggleService,
) {
    route("/api/etteroppgjoer/{$ETTEROPPGJOER_CALL_PARAMETER}") {
        get {
            sjekkEtteroppgjoerEnabled(featureToggleService)
            kunSkrivetilgang {
                val etteroppgjoer = service.hentEtteroppgjoer(etteroppgjoerId)
                call.respond(etteroppgjoer)
            }
        }
    }

    route("/etteroppgjoer/{$SAKID_CALL_PARAMETER}") {
        post {
            sjekkEtteroppgjoerEnabled(featureToggleService)
            kunSkrivetilgang {
                service.opprettEtteroppgjoer(sakId, 2024)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

fun sjekkEtteroppgjoerEnabled(featureToggleService: FeatureToggleService) {
    if (!featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER, false)) {
        throw IkkeTillattException("ETTEROPPGJOER_NOT_ENABLED", "Etteroppgjør er ikke skrudd på i miljøet.")
    }
}
