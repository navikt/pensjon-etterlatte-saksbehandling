package no.nav.etterlatte.behandling.klage

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.KLAGEID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandling.Formkrav
import no.nav.etterlatte.libs.common.behandling.InitieltUtfallMedBegrunnelseDto
import no.nav.etterlatte.libs.common.behandling.InnkommendeKlage
import no.nav.etterlatte.libs.common.behandling.Kabalrespons
import no.nav.etterlatte.libs.common.behandling.KlageUtfallUtenBrev
import no.nav.etterlatte.libs.common.hvisEnabled
import no.nav.etterlatte.libs.common.klage.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.klageId
import no.nav.etterlatte.libs.common.kunSystembruker
import no.nav.etterlatte.libs.common.medBody
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.tilgangsstyring.kunSaksbehandlerMedSkrivetilgang
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang

enum class KlageFeatureToggle(private val key: String) : FeatureToggle {
    KanBrukeKlageToggle("pensjon-etterlatte.kan-bruke-klage"),
    KanFerdigstilleKlageToggle("pensjons-etterlatte.kan-ferdigstille-klage"),
    ;

    override fun key(): String = key
}

internal fun Route.klageRoutes(
    klageService: KlageService,
    featureToggleService: FeatureToggleService,
) {
    route("/api/klage") {
        post("opprett/{$SAKID_CALL_PARAMETER}") {
            kunSkrivetilgang {
                hvisEnabled(featureToggleService, KlageFeatureToggle.KanBrukeKlageToggle) {
                    medBody<InnkommendeKlage> { innkommendeKlage ->
                        val sakId = sakId
                        val klage =
                            inTransaction {
                                klageService.opprettKlage(sakId, innkommendeKlage)
                            }
                        call.respond(klage)
                    }
                }
            }
        }

        route("{$KLAGEID_CALL_PARAMETER}") {
            get {
                hvisEnabled(featureToggleService, KlageFeatureToggle.KanBrukeKlageToggle) {
                    val klage =
                        inTransaction {
                            klageService.hentKlage(klageId)
                        }
                    when (klage) {
                        null -> call.respond(HttpStatusCode.NotFound)
                        else -> call.respond(klage)
                    }
                }
            }

            put("formkrav") {
                hvisEnabled(featureToggleService, KlageFeatureToggle.KanBrukeKlageToggle) {
                    kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                        medBody<VurdereFormkravDto> { formkravDto ->
                            val oppdatertKlage =
                                inTransaction {
                                    klageService.lagreFormkravIKlage(klageId, formkravDto.formkrav, saksbehandler)
                                }
                            call.respond(oppdatertKlage)
                        }
                    }
                }
            }

            put("initieltutfall") {
                hvisEnabled(featureToggleService, KlageFeatureToggle.KanBrukeKlageToggle) {
                    kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                        medBody<InitieltUtfallMedBegrunnelseDto> { utfallMedBegrunnelse ->
                            val oppdatertKlage =
                                inTransaction {
                                    klageService.lagreInitieltUtfallMedBegrunnelseAvKlage(klageId, utfallMedBegrunnelse, saksbehandler)
                                }
                            call.respond(oppdatertKlage)
                        }
                    }
                }
            }

            put("utfall") {
                hvisEnabled(featureToggleService, KlageFeatureToggle.KanBrukeKlageToggle) {
                    kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                        medBody<VurdertUtfallDto> { utfall ->
                            val oppdatertKlage =
                                inTransaction {
                                    klageService.lagreUtfallAvKlage(klageId, utfall.utfall, saksbehandler)
                                }
                            call.respond(oppdatertKlage)
                        }
                    }
                }
            }

            post("ferdigstill") {
                hvisEnabled(featureToggleService, KlageFeatureToggle.KanFerdigstilleKlageToggle) {
                    kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                        val ferdigstiltKlage =
                            inTransaction {
                                klageService.ferdigstillKlage(klageId, saksbehandler)
                            }
                        call.respond(ferdigstiltKlage)
                    }
                }
            }

            patch("kabalstatus") {
                hvisEnabled(featureToggleService, KlageFeatureToggle.KanBrukeKlageToggle) {
                    kunSystembruker {
                        medBody<Kabalrespons> {
                            inTransaction {
                                klageService.haandterKabalrespons(klageId, it)
                            }
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                }
            }

            post("avbryt") {
                kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                    hvisEnabled(featureToggleService, KlageFeatureToggle.KanBrukeKlageToggle) {
                        medBody<AvbrytKlageDto> { avbrytKlageDto ->
                            inTransaction {
                                klageService.avbrytKlage(
                                    klageId,
                                    avbrytKlageDto.aarsakTilAvbrytelse,
                                    avbrytKlageDto.kommentar,
                                    saksbehandler,
                                )
                            }
                        }
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }

        get("sak/{$SAKID_CALL_PARAMETER}") {
            hvisEnabled(featureToggleService, KlageFeatureToggle.KanBrukeKlageToggle) {
                val klager =
                    inTransaction {
                        klageService.hentKlagerISak(sakId)
                    }
                call.respond(klager)
            }
        }
    }
}

data class VurdereFormkravDto(val formkrav: Formkrav)

data class VurdertUtfallDto(val utfall: KlageUtfallUtenBrev)

data class AvbrytKlageDto(val aarsakTilAvbrytelse: AarsakTilAvbrytelse, val kommentar: String)
