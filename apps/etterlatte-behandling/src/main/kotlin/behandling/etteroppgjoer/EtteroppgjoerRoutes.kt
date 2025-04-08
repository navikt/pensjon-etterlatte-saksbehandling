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
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.appIsInGCP
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerFaktiskInntektRequest
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
                val eo =
                    inTransaction {
                        forbehandlingService.opprettEtteroppgjoerForbehandling(
                            sakId,
                            2024,
                            brukerTokenInfo,
                        )
                    }
                call.respond(eo)
            }
        }

        route("/{$ETTEROPPGJOER_CALL_PARAMETER}") {
            get {
                sjekkEtteroppgjoerEnabled(featureToggleService)
                kunSkrivetilgang {
                    val etteroppgjoer =
                        inTransaction {
                            forbehandlingService.hentForbehandlingForFrontend(brukerTokenInfo, etteroppgjoerId)
                        }
                    call.respond(etteroppgjoer)
                }
            }

            post("hent_faktisk_inntekt") {
                val request = call.receive<EtteroppgjoerFaktiskInntektRequest>()

                val response =
                    inTransaction {
                        forbehandlingService.hentFaktiskInntent(request, brukerTokenInfo)
                    }
                if (response == null) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(response)
                }
            }

            post("beregn_faktisk_inntekt") {
                val request = call.receive<BeregnFaktiskInntektRequest>()
                val response =
                    inTransaction {
                        forbehandlingService.lagreOgBeregnFaktiskInntekt(etteroppgjoerId, request, brukerTokenInfo)
                    }
                call.respond(response)
            }
        }

        post("/{$SAKID_CALL_PARAMETER}") {
            sjekkEtteroppgjoerEnabled(featureToggleService)
            kunSkrivetilgang {
                val eo =
                    inTransaction {
                        forbehandlingService.opprettEtteroppgjoerForbehandling(sakId, 2024, brukerTokenInfo)
                    }
                call.respond(eo)
            }
        }

        get("/forbehandlinger/{$SAKID_CALL_PARAMETER}") {
            sjekkEtteroppgjoerEnabled(featureToggleService)
            val forbehandlinger = inTransaction { forbehandlingService.hentEtteroppgjoerForbehandlinger(sakId) }
            call.respond(forbehandlinger)
        }

        // TODO opprett periodisk jobb
        post("/skatteoppgjoerhendelser/start-kjoering") {
            sjekkEtteroppgjoerEnabled(featureToggleService)
            kunSystembruker {
                val request = call.receive<HendelseKjoeringRequest>()
                inTransaction {
                    skatteoppgjoerHendelserService.startHendelsesKjoering(request)
                }
                call.respond(HttpStatusCode.OK)
            }
        }

        // TODO opprett periodisk jobb
        post("/{inntektsaar}/start-kjoering") {
            sjekkEtteroppgjoerEnabled(featureToggleService)
            kunSystembruker {
                val inntektsaar =
                    krevIkkeNull(call.parameters["inntektsaar"]?.toInt()) {
                        "Inntektsaar mangler"
                    }
                inTransaction {
                    etteroppgjoerService.finnOgOpprettEtteroppgjoer(inntektsaar)
                }
                call.respond(HttpStatusCode.OK)
            }
        }

        // TODO opprett periodisk jobb
        post("/{inntektsaar}/opprett-forbehandlinger") {
            sjekkEtteroppgjoerEnabled(featureToggleService)
            kunSystembruker {
                val inntektsaar =
                    krevIkkeNull(call.parameters["inntektsaar"]?.toInt()) {
                        "Inntektsaar mangler"
                    }

                forbehandlingService.startOpprettForbehandlingKjoering(inntektsaar)
            }
        }
    }
}

private fun sjekkEtteroppgjoerEnabled(featureToggleService: FeatureToggleService) {
    if (!featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER, false)) {
        throw IkkeTillattException("ETTEROPPGJOER_NOT_ENABLED", "Etteroppgjør er ikke skrudd på i miljøet.")
    }
}
