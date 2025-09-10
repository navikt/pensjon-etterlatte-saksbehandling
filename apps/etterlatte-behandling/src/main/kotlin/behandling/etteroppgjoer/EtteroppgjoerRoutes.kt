package no.nav.etterlatte.behandling.etteroppgjoer

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.etteroppgjoer.brev.EtteroppgjoerForbehandlingBrevService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.BeregnFaktiskInntektRequest
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.InformasjonFraBrukerRequest
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.HendelseKjoeringRequest
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.HendelserSettSekvensnummerRequest
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SkatteoppgjoerHendelserService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.appIsInGCP
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.AvbrytForbehandlingRequest
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.isDev
import no.nav.etterlatte.libs.ktor.route.FORBEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.forbehandlingId
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang

enum class EtteroppgjoerToggles(
    private val toggle: String,
) : FeatureToggle {
    ETTEROPPGJOER("etteroppgjoer"),
    ETTEROPPGJOER_STUB_INNTEKT("etteroppgjoer_stub_inntekt"),
    ETTEROPPGJOER_STUB_PGI("etteroppgjoer_stub_pgi"),
    ETTEROPPGJOER_STUB_HENDELSER("etteroppgjoer_stub_hendelser"),
    ETTEROPPGJOER_PERIODISK_JOBB("etteroppgjoer_periodisk_jobb"),
    ETTEROPPGJOER_SKATTEHENDELSES_JOBB("etteroppgjoer_skattehendelses_jobb"),
    ETTEROPPGJOER_SVARFRISTUTLOEPT_JOBB("etteroppgjoer_svarfristutloept_jobb"),
    ;

    override fun key(): String = toggle
}

fun Route.etteroppgjoerRoutes(
    forbehandlingService: EtteroppgjoerForbehandlingService,
    forbehandlingBrevService: EtteroppgjoerForbehandlingBrevService,
    etteroppgjoerService: EtteroppgjoerService,
    skatteoppgjoerHendelserService: SkatteoppgjoerHendelserService,
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

        route("/{$SAKID_CALL_PARAMETER}") {
            get {
                sjekkEtteroppgjoerEnabled(featureToggleService)
                kunSkrivetilgang {
                    val etteroppgjoer =
                        inTransaction {
                            etteroppgjoerService.hentAlleAktiveEtteroppgjoerForSak(sakId)
                        }
                    call.respond(etteroppgjoer)
                }
            }
        }

        route("/forbehandling/{$FORBEHANDLINGID_CALL_PARAMETER}") {
            get {
                sjekkEtteroppgjoerEnabled(featureToggleService)
                kunSkrivetilgang {
                    val etteroppgjoer =
                        inTransaction {
                            forbehandlingService.hentDetaljertForbehandling(forbehandlingId, brukerTokenInfo)
                        }
                    call.respond(etteroppgjoer)
                }
            }

            post("beregn-faktisk-inntekt") {
                val request = call.receive<BeregnFaktiskInntektRequest>()
                val response =
                    inTransaction {
                        forbehandlingService.lagreOgBeregnFaktiskInntekt(forbehandlingId, request, brukerTokenInfo)
                    }
                call.respond(response)
            }

            post("ferdigstill") {
                sjekkEtteroppgjoerEnabled(featureToggleService)
                kunSkrivetilgang {
                    inTransaction {
                        runBlocking {
                            forbehandlingBrevService.ferdigstillForbehandlingOgDistribuerBrev(
                                forbehandlingId,
                                brukerTokenInfo,
                            )
                        }
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }

            post("avbryt") {
                sjekkEtteroppgjoerEnabled(featureToggleService)
                kunSkrivetilgang {
                    val body = call.receive<AvbrytForbehandlingRequest>()
                    inTransaction {
                        forbehandlingService.avbrytForbehandling(
                            forbehandlingId,
                            brukerTokenInfo,
                            body.aarsakTilAvbrytelse,
                            body.kommentar,
                        )
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }

            post("informasjon-fra-bruker") {
                val request = call.receive<InformasjonFraBrukerRequest>()

                val response =
                    inTransaction {
                        forbehandlingService.lagreInformasjonFraBruker(
                            forbehandlingId = forbehandlingId,
                            harMottattNyInformasjon = request.harMottattNyInformasjon,
                            endringErTilUgunstForBruker = request.endringErTilUgunstForBruker,
                            beskrivelseAvUgunst = request.beskrivelseAvUgunst,
                        )
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

        post("/les-skatteoppgjoer-hendelser") {
            sjekkEtteroppgjoerEnabled(featureToggleService)

            kunSystembruker {
                val hendelseKjoeringRequest: HendelseKjoeringRequest = call.receive()
                skatteoppgjoerHendelserService.lesOgBehandleHendelser(hendelseKjoeringRequest)

                call.respond(HttpStatusCode.OK)
            }
        }

        post("/sett-skatteoppgjoer-sekvensnummer") {
            sjekkEtteroppgjoerEnabled(featureToggleService)

            kunSystembruker {
                val request: HendelserSettSekvensnummerRequest = call.receive()
                skatteoppgjoerHendelserService.settSekvensnummerForLesingFraDato(request.startdato)
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
