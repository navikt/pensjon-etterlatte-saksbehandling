package no.nav.etterlatte.behandling.klage

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
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
import no.nav.etterlatte.libs.common.behandling.Formkrav
import no.nav.etterlatte.libs.common.behandling.InitieltUtfallMedBegrunnelseDto
import no.nav.etterlatte.libs.common.behandling.InnkommendeKlage
import no.nav.etterlatte.libs.common.behandling.Kabalrespons
import no.nav.etterlatte.libs.common.behandling.KlageUtfallUtenBrev
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.klage.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.route.KLAGEID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.OPPGAVEID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.klageId
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.medBody
import no.nav.etterlatte.libs.ktor.route.oppgaveId
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.tilgangsstyring.kunSaksbehandlerMedSkrivetilgang
import java.time.LocalDate
import java.time.OffsetDateTime

enum class KlageFeatureToggle(
    private val key: String,
) : FeatureToggle {
    KanOppretteVedtakAvvisningToggle("pensjon-etterlatte.kan-opprette-vedtak-avvist-klage"),
    StoetterUtfallDelvisOmgjoering("pensjon-etterlatte.klage-delvis-omgjoering"),
    ;

    override fun key(): String = key
}

internal fun Route.klageRoutes(
    klageService: KlageService,
    featureToggleService: FeatureToggleService,
) {
    route("/api/klage") {
        post("opprett/{$SAKID_CALL_PARAMETER}") {
            kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                medBody<InnkommendeKlageDto> { dto ->
                    val sakId = sakId
                    val klage =
                        inTransaction {
                            klageService.opprettKlage(
                                sakId,
                                InnkommendeKlage(
                                    mottattDato = dto.parseMottattDato(),
                                    journalpostId = dto.journalpostId,
                                    innsender = dto.innsender,
                                ),
                                saksbehandler,
                            )
                        }
                    call.respond(klage)
                }
            }
        }

        route("omgjoering/{$OPPGAVEID_CALL_PARAMETER}/avslutt") {
            post {
                kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                    medBody<AvsluttOmgjoeringOppgaveDto> { request ->
                        val oppdatertOppgave =
                            inTransaction {
                                klageService.avsluttOmgjoeringsoppgave(
                                    oppgaveId = oppgaveId,
                                    grunnForAvslutning = request.hvorforAvsluttes,
                                    begrunnelse = request.begrunnelse,
                                    omgjoerendeBehandling = request.omgjoerendeBehandling,
                                    saksbehandler = saksbehandler,
                                )
                            }
                        call.respond(oppdatertOppgave)
                    }
                }
            }
        }

        route("{$KLAGEID_CALL_PARAMETER}") {
            get {
                val klage =
                    inTransaction {
                        klageService.hentKlage(klageId)
                    } ?: throw GenerellIkkeFunnetException()

                call.respond(klage)
            }

            put("formkrav") {
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

            put("klager-ikke-svart") {
                kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                    medBody<KlagerHarIkkeSvartDto> { ikkeSvartDto ->
                        val oppdatertKlage =
                            inTransaction {
                                klageService.oppdaterKlagerIkkeSvart(klageId, ikkeSvartDto.begrunnelse, saksbehandler)
                            }
                        call.respond(oppdatertKlage)
                    }
                }
            }

            put("initieltutfall") {
                kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                    medBody<InitieltUtfallMedBegrunnelseDto> { utfallMedBegrunnelse ->
                        val oppdatertKlage =
                            inTransaction {
                                klageService.lagreInitieltUtfallMedBegrunnelseAvKlage(
                                    klageId,
                                    utfallMedBegrunnelse,
                                    saksbehandler,
                                )
                            }
                        call.respond(oppdatertKlage)
                    }
                }
            }

            put("utfall") {
                kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                    medBody<VurdertUtfallDto> { utfall ->
                        sjekkStoetterUtfallHvisAvvist(featureToggleService, utfall)
                        val oppdatertKlage =
                            inTransaction {
                                klageService.lagreUtfallAvKlage(klageId, utfall.utfall, saksbehandler)
                            }
                        call.respond(oppdatertKlage)
                    }
                }
            }

            post("ferdigstill") {
                kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                    val ferdigstiltKlage =
                        inTransaction {
                            klageService.ferdigstillKlage(klageId, saksbehandler)
                        }
                    call.respond(ferdigstiltKlage)
                }
            }

            patch("kabalstatus") {
                kunSystembruker {
                    medBody<Kabalrespons> {
                        inTransaction {
                            klageService.haandterKabalrespons(klageId, it)
                        }
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            put("mottattdato") {
                kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                    medBody<OppdaterMottattDatoRequest> {
                        val klage =
                            inTransaction {
                                klageService.oppdaterMottattDato(klageId, it.parseMottattDato(), saksbehandler)
                            }
                        call.respond(klage)
                    }
                }
            }

            post("avbryt") {
                kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
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

            route("vedtak") {
                post("omgjoering") {
                    kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                        inTransaction {
                            klageService.opprettOppgaveForOmgjoering(klageId, saksbehandler)
                        }
                        call.respond(HttpStatusCode.OK)
                    }
                }

                post("fatt") {
                    kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                        val klage =
                            inTransaction {
                                klageService.fattVedtak(klageId, saksbehandler)
                            }
                        call.respond(HttpStatusCode.OK, klage)
                    }
                }

                post("attester") {
                    kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                        val (kommentar) = call.receive<KlageAttesterRequest>()
                        val klage =
                            inTransaction {
                                klageService.attesterVedtak(klageId, kommentar, saksbehandler)
                            }
                        call.respond(HttpStatusCode.OK, klage)
                    }
                }

                post("underkjenn") {
                    kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                        val (kommentar, valgtBegrunnelse) = call.receive<KlageUnderkjennRequest>()
                        val klage =
                            inTransaction {
                                klageService.underkjennVedtak(klageId, kommentar, valgtBegrunnelse, saksbehandler)
                            }
                        call.respond(HttpStatusCode.OK, klage)
                    }
                }
            }
        }

        get("sak/{$SAKID_CALL_PARAMETER}") {
            val klager =
                inTransaction {
                    klageService.hentKlagerISak(sakId)
                }
            call.respond(klager)
        }
    }
}

enum class GrunnForAvslutning(
    val lesbarBeskrivelse: String,
) {
    OMGJORT_ALLEREDE("Allerede omgjort"),
    OMGJOERINGSOPPGAVE_OPPRETTET_VED_FEIL("Oppgaven opprettet ved en feil"),
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class AvsluttOmgjoeringOppgaveDto(
    val omgjoerendeBehandling: String?,
    val begrunnelse: String,
    val hvorforAvsluttes: GrunnForAvslutning,
)

data class OppdaterMottattDatoRequest(
    val mottattDato: String,
) {
    fun parseMottattDato(): LocalDate = Tidspunkt(OffsetDateTime.parse(mottattDato).toInstant()).toNorskLocalDate()
}

private fun sjekkStoetterUtfallHvisAvvist(
    featureToggleService: FeatureToggleService,
    utfall: VurdertUtfallDto,
) {
    val vedtakAktivert =
        featureToggleService
            .isEnabled(KlageFeatureToggle.KanOppretteVedtakAvvisningToggle, false)
    if (utfall.utfall is KlageUtfallUtenBrev.Avvist && !vedtakAktivert) {
        throw IkkeTillattException(
            "KLAGE_KAN_IKKE_AVVISES_MED_VEDTAK",
            "Avvisning med vedtak er ikke aktivert ennå",
        )
    }
}

data class InnkommendeKlageDto(
    val mottattDato: String,
    val journalpostId: String,
    val innsender: String?,
) {
    fun parseMottattDato(): LocalDate = Tidspunkt(OffsetDateTime.parse(mottattDato).toInstant()).toNorskLocalDate()
}

data class VurdereFormkravDto(
    val formkrav: Formkrav,
)

data class VurdertUtfallDto(
    val utfall: KlageUtfallUtenBrev,
)

data class AvbrytKlageDto(
    val aarsakTilAvbrytelse: AarsakTilAvbrytelse,
    val kommentar: String,
)

data class KlageAttesterRequest(
    val kommentar: String,
)

data class KlageUnderkjennRequest(
    val kommentar: String,
    val valgtBegrunnelse: String,
)

data class KlagerHarIkkeSvartDto(
    val begrunnelse: String,
)
