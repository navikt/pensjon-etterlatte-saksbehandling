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
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.EtteroppgjoerFilter
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.appIsInGCP
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.AvbrytForbehandlingRequest
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.isDev
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.route.FORBEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.OPPGAVEID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.forbehandlingId
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.oppgaveId
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.logger
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
    ETTEROPPGJOER_STARTPUNKT_SKATTEHENDELSES_JOBB("etteroppgjoer_startpunkt_skattehendelses_jobb"),
    ETTEROPPGJOER_SVARFRISTUTLOEPT_JOBB("etteroppgjoer_svarfristutloept_jobb"),
    ETTEROPPGJOER_OPPRETT_FORBEHANDLING_JOBB("etteroppgjoer_opprett_forbehandling_jobb"),
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
        route("/{$SAKID_CALL_PARAMETER}") {
            get {
                sjekkEtteroppgjoerEnabled(featureToggleService)
                kunSkrivetilgang {
                    val etteroppgjoer =
                        inTransaction {
                            etteroppgjoerService.hentAlleAktiveEtteroppgjoerForSak(sakId)
                        }

                    if (etteroppgjoer == null) {
                        call.respond(HttpStatusCode.NotFound, "Fant ikke etteroppgjør for sak $sakId")
                    } else {
                        call.respond(etteroppgjoer)
                    }
                }
            }

            post("/kundev-opprett-forbehandling") {
                sjekkEtteroppgjoerEnabled(featureToggleService)
                if (appIsInGCP() && !isDev()) {
                    call.respond(HttpStatusCode.NotFound)
                }
                kunSkrivetilgang {
                    inTransaction {
                        val etteroppgjoer = etteroppgjoerService.hentAlleAktiveEtteroppgjoerForSak(sakId)
                        if (etteroppgjoer?.status == EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER) {
                            etteroppgjoerService.oppdaterEtteroppgjoerStatus(
                                sakId,
                                etteroppgjoer.inntektsaar,
                                EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
                            )
                        }

                        forbehandlingService.opprettOppgaveForOpprettForbehandling(
                            sakId,
                        )
                    }
                    // TODO: returnere oppgave?
                    call.respond(HttpStatusCode.OK)
                }
            }

            post("/forbehandling/{$OPPGAVEID_CALL_PARAMETER}") {
                sjekkEtteroppgjoerEnabled(featureToggleService)
                kunSkrivetilgang {
                    val forbehandling =
                        inTransaction {
                            forbehandlingService.opprettEtteroppgjoerForbehandling(sakId, 2024, oppgaveId, brukerTokenInfo)
                        }
                    call.respond(forbehandling)
                }
            }
        }

        route("/forbehandling") {
            route("/{$FORBEHANDLINGID_CALL_PARAMETER}") {
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
                    val (etteroppgjoerResultatDto, brevSomskalSlettes) =
                        inTransaction {
                            forbehandlingService.lagreOgBeregnFaktiskInntekt(forbehandlingId, request, brukerTokenInfo)
                        }

                    if (brevSomskalSlettes != null) {
                        logger.info(
                            "Sletter brevet koblet til forbehandlingen med brevId=${brevSomskalSlettes.first} " +
                                "i sak=${brevSomskalSlettes.second}",
                        )
                        forbehandlingBrevService.slettVarselbrev(
                            brevSomskalSlettes = brevSomskalSlettes.first,
                            sakId = brevSomskalSlettes.second,
                            brukerTokenInfo = brukerTokenInfo,
                        )
                    }

                    call.respond(etteroppgjoerResultatDto)
                }

                post("ferdigstill") {
                    sjekkEtteroppgjoerEnabled(featureToggleService)
                    kunSkrivetilgang {
                        inTransaction {
                            runBlocking {
                                forbehandlingBrevService.ferdigstillForbehandlingMedBrev(
                                    forbehandlingId,
                                    brukerTokenInfo,
                                )
                            }
                        }
                        call.respond(HttpStatusCode.OK)
                    }
                }

                post("ferdigstill-uten-brev") {
                    sjekkEtteroppgjoerEnabled(featureToggleService)
                    kunSkrivetilgang {
                        val forbehandling =
                            inTransaction {
                                runBlocking {
                                    forbehandlingService.ferdigstillForbehandlingUtenBrev(forbehandlingId, brukerTokenInfo)
                                }
                            }
                        call.respond(forbehandling)
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

                    inTransaction {
                        forbehandlingService.lagreInformasjonFraBruker(
                            forbehandlingId = forbehandlingId,
                            harMottattNyInformasjon = request.harMottattNyInformasjon,
                            endringErTilUgunstForBruker = request.endringErTilUgunstForBruker,
                            beskrivelseAvUgunst = request.beskrivelseAvUgunst,
                        )
                    }

                    call.respond(HttpStatusCode.OK)
                }
            }

            post("bulk") {
                sjekkEtteroppgjoerEnabled(featureToggleService)

                kunSystembruker {
                    val request = call.receive<EtteroppgjoerForbehandlingBulkRequest>()
                    logger.info("Starter bulk opprettelse av etteroppgjør forbehandlinger")

                    inTransaction {
                        forbehandlingService.opprettEtteroppgjoerForbehandlingIBulk(
                            inntektsaar = request.inntektsaar,
                            antall = request.antall,
                            etteroppgjoerFilter = request.etteroppgjoerFilter,
                            spesifikkeSaker = request.spesifikkeSaker,
                            ekskluderteSaker = request.ekskluderteSaker,
                        )
                    }

                    logger.info("Ferdig med bulk opprettelse av etteroppgjør forbehandlinger")

                    call.respond(HttpStatusCode.OK)
                }
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

data class EtteroppgjoerForbehandlingBulkRequest(
    val inntektsaar: Int,
    val antall: Int,
    val etteroppgjoerFilter: EtteroppgjoerFilter,
    val spesifikkeSaker: List<SakId>,
    val ekskluderteSaker: List<SakId>,
)
