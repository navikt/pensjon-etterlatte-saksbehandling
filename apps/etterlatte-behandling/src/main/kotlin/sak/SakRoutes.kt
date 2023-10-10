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
import no.nav.etterlatte.behandling.domain.toBehandlingSammendrag
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringsListe
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.common.kunSaksbehandler
import no.nav.etterlatte.libs.common.kunSystembruker
import no.nav.etterlatte.libs.common.oppgave.OppgaveListe
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

            get("/behandlinger/foerstevirk") {
                logger.info("Henter første virkningstidspunkt på en iverksatt behandling i sak med id $sakId")
                when (val foersteVirk = inTransaction { behandlingService.hentFoersteVirk(sakId) }) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(FoersteVirkDto(foersteVirk.atDay(1), sakId))
                }
            }
        }

        route("/personer/") {
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

            post("behandlinger") {
                withFoedselsnummerInternal(tilgangService) { fnr ->
                    val behandlinger =
                        inTransaction {
                            sakService.finnSaker(fnr.value)
                                .map { sak ->
                                    behandlingService.hentBehandlingerISak(sak.id)
                                        .map { it.toBehandlingSammendrag() }
                                        .let { BehandlingListe(sak, it) }
                                }
                        }.also { requestLogger.loggRequest(brukerTokenInfo, fnr, "behandlinger") }
                    call.respond(behandlinger)
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

data class FoersteVirkDto(val foersteIverksatteVirkISak: LocalDate, val sakId: Long)
