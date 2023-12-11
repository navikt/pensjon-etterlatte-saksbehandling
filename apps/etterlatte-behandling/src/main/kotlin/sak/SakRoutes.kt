package no.nav.etterlatte.sak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.BehandlingListe
import no.nav.etterlatte.behandling.BehandlingRequestLogger
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.behandling.domain.toBehandlingSammendrag
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringsListe
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandling.ForenkletBehandling
import no.nav.etterlatte.libs.common.behandling.ForenkletBehandlingListeWrapper
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.hentNavidentFraToken
import no.nav.etterlatte.libs.common.kunSaksbehandler
import no.nav.etterlatte.libs.common.kunSystembruker
import no.nav.etterlatte.libs.common.oppgave.OppgaveListe
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.Saker
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.tilgangsstyring.withFoedselsnummerAndGradering
import no.nav.etterlatte.tilgangsstyring.withFoedselsnummerInternal
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal fun Route.sakSystemRoutes(
    tilgangService: TilgangService,
    sakService: SakService,
    behandlingService: BehandlingService,
    requestLogger: BehandlingRequestLogger,
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    route("/saker") {
        get {
            kunSystembruker {
                call.respond(Saker(inTransaction { sakService.hentSaker() }))
            }
        }

        route("/{$SAKID_CALL_PARAMETER}") {
            get {
                val sak =
                    inTransaction {
                        sakService.finnSak(sakId)
                    }
                call.respond(sak ?: HttpStatusCode.NotFound)
            }

            // TODO: Fjerne når grunnlag er versjonert (EY-2567)
            get("/behandlinger") {
                kunSystembruker {
                    val behandlinger =
                        inTransaction { behandlingService.hentBehandlingerForSak(sakId) }
                            .map { ForenkletBehandling(it.sak.id, it.id, it.status) }

                    call.respond(ForenkletBehandlingListeWrapper(behandlinger))
                }
            }

            get("/behandlinger/sisteIverksatte") {
                logger.info("Henter siste iverksatte behandling for $sakId")

                val sisteIverksatteBehandling =
                    inTransaction {
                        behandlingService.hentSisteIverksatte(sakId)
                            ?.let { SisteIverksatteBehandling(it.id) }
                    }

                call.respond(sisteIverksatteBehandling ?: HttpStatusCode.NotFound)
            }
        }
    }

    post("personer/saker/{type}") {
        withFoedselsnummerAndGradering(tilgangService) { fnr, gradering ->
            val type: SakType = enumValueOf(requireNotNull(call.parameters["type"]) { "Må ha en Saktype for å finne eller opprette sak" })
            val message = inTransaction { sakService.finnEllerOpprettSak(fnr = fnr.value, type, gradering = gradering) }
            requestLogger.loggRequest(brukerTokenInfo, fnr, "personer/saker")
            call.respond(message)
        }
    }

    post("personer/getsak/{type}") {
        withFoedselsnummerInternal(tilgangService) { fnr ->
            val type: SakType = enumValueOf(requireNotNull(call.parameters["type"]) { "Må ha en Saktype for å finne sak" })
            val sak =
                inTransaction { sakService.finnSak(fnr.value, type) }.also {
                    requestLogger.loggRequest(brukerTokenInfo, fnr, "personer/sak")
                }
            call.respond(sak ?: HttpStatusCode.NotFound)
        }
    }
}

class PersonManglerSak(message: String) :
    UgyldigForespoerselException(
        code = "PERSON_MANGLER_SAK",
        detail = message,
    )

class SakIkkeFunnetException(message: String) :
    UgyldigForespoerselException(
        code = "FANT_INGEN_SAK",
        detail = message,
    )

class SakBleBorteException(message: String) :
    IkkeTillattException(
        code = "SAKEN_FORSVANT",
        detail = message,
    )

internal fun Route.sakWebRoutes(
    tilgangService: TilgangService,
    sakService: SakService,
    behandlingService: BehandlingService,
    grunnlagsendringshendelseService: GrunnlagsendringshendelseService,
    oppgaveService: OppgaveService,
    requestLogger: BehandlingRequestLogger,
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    route("/api") {
        route("/sak/{$SAKID_CALL_PARAMETER}") {
            get {
                val sak =
                    inTransaction {
                        sakService.finnSak(sakId)
                    }
                call.respond(sak ?: HttpStatusCode.NotFound)
            }

            get("/grunnlagsendringshendelser") {
                call.respond(inTransaction { GrunnlagsendringsListe(grunnlagsendringshendelseService.hentAlleHendelserForSak(sakId)) })
            }

            post("/endre_enhet") {
                hentNavidentFraToken { navIdent ->
                    val enhetrequest = call.receive<EnhetRequest>()
                    try {
                        inTransaction { sakService.finnSak(sakId) }
                            ?: throw SakIkkeFunnetException("Fant ingen sak å endre enhet på sakid: $sakId")

                        val sakMedEnhet =
                            GrunnlagsendringshendelseService.SakMedEnhet(
                                enhet = enhetrequest.enhet,
                                id = sakId,
                            )

                        inTransaction {
                            sakService.oppdaterEnhetForSaker(listOf(sakMedEnhet))
                            oppgaveService.oppdaterEnhetForRelaterteOppgaver(listOf(sakMedEnhet))
                        }

                        logger.info(
                            "Saksbehandler $navIdent endret enhet på sak: $sakId og tilhørende oppgaver til enhet: ${sakMedEnhet.enhet}",
                        )
                        val oppdatertSak =
                            inTransaction { sakService.finnSak(sakId) }
                                ?: throw SakBleBorteException("Saken ble borte etter å endre enheten på den sakid: $sakId. Kritisk! ")

                        call.respond(oppdatertSak)
                    } catch (e: TilstandException.UgyldigTilstand) {
                        call.respond(HttpStatusCode.BadRequest, "Kan ikke endre enhet på sak og oppgaver")
                    }
                }
            }

            get("/behandlinger/foerstevirk") {
                logger.info("Henter første virkningstidspunkt på en iverksatt behandling i sak med id $sakId")
                when (val foersteVirk = inTransaction { behandlingService.hentFoersteVirk(sakId) }) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(FoersteVirkDto(foersteVirk.atDay(1), sakId))
                }
            }
        }

        route("/personer/") {
            post("/navkontor") {
                withFoedselsnummerInternal(tilgangService) { fnr ->
                    val navkontor = sakService.finnNavkontorForPerson(fnr.value)
                    call.respond(navkontor)
                }
            }

            post("/behandlingerforsak") {
                withFoedselsnummerInternal(tilgangService) { fnr ->
                    val behandlinger =
                        inTransaction {
                            val sak = sakService.hentEnkeltSakForPerson(fnr.value)
                            val utlandstilknytning = behandlingService.hentUtlandstilknytningForSak(sak.id)
                            val sakMedUtlandstilknytning = SakMedUtlandstilknytning.fra(sak, utlandstilknytning)

                            requestLogger.loggRequest(
                                brukerTokenInfo,
                                Folkeregisteridentifikator.of(sak.ident),
                                "behandlinger",
                            )

                            behandlingService.hentBehandlingerForSak(sakMedUtlandstilknytning.id)
                                .map { it.toBehandlingSammendrag() }
                                .let { BehandlingListe(sakMedUtlandstilknytning, it) }
                        }
                    call.respond(behandlinger)
                }
            }

            post("sak/{type}") {
                withFoedselsnummerInternal(tilgangService) { fnr ->
                    val type: SakType =
                        requireNotNull(call.parameters["type"]) {
                            "Mangler påkrevd parameter {type} for å hente sak på bruker"
                        }.let { enumValueOf(it) }

                    val sak =
                        inTransaction { sakService.finnSak(fnr.value, type) }
                            .also { requestLogger.loggRequest(brukerTokenInfo, fnr, "personer/sak/type") }
                    call.respond(sak ?: HttpStatusCode.NoContent)
                }
            }

            post("oppgaver") {
                withFoedselsnummerInternal(tilgangService) { fnr ->
                    val oppgaver =
                        inTransaction {
                            sakService.finnSaker(fnr.value)
                                .map { sak ->
                                    OppgaveListe(sak, oppgaveService.hentOppgaverForSak(sak.id))
                                }
                        }.also { requestLogger.loggRequest(brukerTokenInfo, fnr, "oppgaver") }
                    call.respond(oppgaver)
                }
            }

            post("grunnlagsendringshendelser") {
                withFoedselsnummerInternal(tilgangService) { fnr ->
                    call.respond(
                        inTransaction {
                            sakService.finnSaker(fnr.value).map { sak ->
                                GrunnlagsendringsListe(grunnlagsendringshendelseService.hentAlleHendelserForSak(sak.id))
                            }
                        }.also { requestLogger.loggRequest(brukerTokenInfo, fnr, "grunnlagsendringshendelser") },
                    )
                }
            }

            post("lukkgrunnlagsendringshendelse") {
                kunSaksbehandler { saksbehandler ->
                    val lukketHendelse = call.receive<Grunnlagsendringshendelse>()
                    grunnlagsendringshendelseService.lukkHendelseMedKommentar(hendelse = lukketHendelse, saksbehandler)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

data class UtlandstilknytningRequest(
    val utlandstilknytningType: UtlandstilknytningType,
    val begrunnelse: String,
)

data class EnhetRequest(
    val enhet: String,
)

data class FoersteVirkDto(val foersteIverksatteVirkISak: LocalDate, val sakId: Long)
