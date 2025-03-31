package no.nav.etterlatte.behandling.etteroppgjoer

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.BeregnFaktiskInntektRequest
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.HendelseKjoeringRequest
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SkatteoppgjoerHendelserService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.appIsInGCP
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.isDev
import no.nav.etterlatte.libs.ktor.route.ETTEROPPGJOER_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.etteroppgjoerId
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang

enum class EtteroppgjoerToggles(
    private val toggle: String,
) : FeatureToggle {
    ETTEROPPGJOER("etteroppgjoer"),
    ETTEROPPGJOER_STUB_INNTEKT("etteroppgjoer_stub_inntekt"),
    ETTEROPPGJOER_STUB_HENDELSER("etteroppgjoer_stub_hendelser"),
    ETTEROPPGJOER_PERIODISK_JOBB("ettteroppgjoer_periodisk_jobb"),
    ;

    override fun key(): String = toggle
}

fun Route.etteroppgjoerRoutes(
    forbehandlingService: EtteroppgjoerForbehandlingService,
    skatteoppgjoerHendelserService: SkatteoppgjoerHendelserService,
    etteroppgjoerService: EtteroppgjoerService,
    featureToggleService: FeatureToggleService,
) {
    route("/api/etteroppgjoer") {
        post("/kundev/{$SAKID_CALL_PARAMETER}") {
            sjekkEtteroppgjoerEnabled(featureToggleService)
            if (appIsInGCP() && !isDev()) {
                call.respond(HttpStatusCode.NotFound)
            }
            kunSkrivetilgang {
                val eo = forbehandlingService.opprettEtteroppgjoer(sakId, 2024, brukerTokenInfo)
                call.respond(eo)
            }
        }

        route("/{$ETTEROPPGJOER_CALL_PARAMETER}") {
            get {
                sjekkEtteroppgjoerEnabled(featureToggleService)
                kunSkrivetilgang {
                    val etteroppgjoer = forbehandlingService.hentEtteroppgjoer(brukerTokenInfo, etteroppgjoerId)
                    call.respond(etteroppgjoer)
                }
            }

            post("beregn_faktisk_inntekt") {
                val request = call.receive<BeregnFaktiskInntektRequest>()
                forbehandlingService.beregnFaktiskInntekt(etteroppgjoerId, request, brukerTokenInfo)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/{$SAKID_CALL_PARAMETER}") {
            sjekkEtteroppgjoerEnabled(featureToggleService)
            kunSkrivetilgang {
                val eo = forbehandlingService.opprettEtteroppgjoer(sakId, 2024, brukerTokenInfo)
                call.respond(eo)
            }
        }

        get("/forbehandlinger/{$SAKID_CALL_PARAMETER}") {
            sjekkEtteroppgjoerEnabled(featureToggleService)
            val forbehandlinger = forbehandlingService.hentEtteroppgjoerForbehandlinger(sakId)
            call.respond(forbehandlinger)
        }

        post("/skatteoppgjoerhendelser/start-kjoering") {
            sjekkEtteroppgjoerEnabled(featureToggleService)
            kunSystembruker {
                val request = call.receive<HendelseKjoeringRequest>()
                skatteoppgjoerHendelserService.startHendelsesKjoering(request)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/{inntektsaar}/start-kjoering") {
            sjekkEtteroppgjoerEnabled(featureToggleService)
            kunSystembruker {
                val inntektsaar =
                    krevIkkeNull(call.parameters["inntektsaar"]?.toInt()) {
                        "Inntektsaar mangler"
                    }
                etteroppgjoerService.finnOgOpprettEtteroppgjoer(inntektsaar)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

private fun sjekkEtteroppgjoerEnabled(featureToggleService: FeatureToggleService) {
    if (!featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER, false)) {
        throw IkkeTillattException("ETTEROPPGJOER_NOT_ENABLED", "Etteroppgjør er ikke skrudd på i miljøet.")
    }
}
