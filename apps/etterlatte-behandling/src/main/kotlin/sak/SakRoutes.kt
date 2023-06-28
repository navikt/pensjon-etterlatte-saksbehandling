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
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.domain.toBehandlingSammendrag
import no.nav.etterlatte.behandling.domain.toDetaljertBehandling
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringsListe
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.kunSystembruker
import no.nav.etterlatte.libs.common.sak.Saker
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.tilgangsstyring.withFoedselsnummerAndGradering
import no.nav.etterlatte.tilgangsstyring.withFoedselsnummerInternal
import org.slf4j.LoggerFactory

internal fun Route.sakSystemRoutes(
    tilgangService: TilgangService,
    sakService: SakService,
    generellBehandlingService: GenerellBehandlingService
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
                val sak = inTransaction {
                    sakService.finnSak(sakId)
                }
                call.respond(sak ?: HttpStatusCode.NotFound)
            }

            get("/behandlinger/sisteIverksatte") {
                logger.info("Henter siste iverksatte behandling for $sakId")
                when (val sisteIverksatteBehandling = generellBehandlingService.hentSisteIverksatte(sakId)) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(sisteIverksatteBehandling.toDetaljertBehandling())
                }
            }
        }
    }

    post("personer/saker/{type}") {
        withFoedselsnummerAndGradering(tilgangService) { fnr, gradering ->
            val type: SakType = enumValueOf(requireNotNull(call.parameters["type"]))
            call.respond(sakService.finnEllerOpprettSak(fnr = fnr.value, type, gradering = gradering))
        }
    }

    post("personer/getsak/{type}") {
        withFoedselsnummerInternal(tilgangService) { fnr ->
            val type: SakType = enumValueOf(requireNotNull(call.parameters["type"]))
            val sak = inTransaction { sakService.finnSak(fnr.value, type) }
            call.respond(sak ?: HttpStatusCode.NotFound)
        }
    }
}

internal fun Route.sakWebRoutes(
    tilgangService: TilgangService,
    sakService: SakService,
    generellBehandlingService: GenerellBehandlingService,
    grunnlagsendringshendelseService: GrunnlagsendringshendelseService
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    route("/api") {
        route("/saker") {
            get("/behandlinger/sisteIverksatte") {
                logger.info("Henter siste iverksatte behandling for $sakId")
                when (val sisteIverksatteBehandling = generellBehandlingService.hentSisteIverksatte(sakId)) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(sisteIverksatteBehandling.toDetaljertBehandling())
                }
            }
        }

        route("/sak/{$SAKID_CALL_PARAMETER}") {
            get {
                val sak = inTransaction {
                    sakService.finnSak(sakId)
                }
                call.respond(sak ?: HttpStatusCode.NotFound)
            }
        }
        route("/personer/") {
            post("behandlinger") {
                withFoedselsnummerInternal(tilgangService) { fnr ->
                    val behandlinger = sakService.finnSaker(fnr.value)
                        .map { sak ->
                            generellBehandlingService.hentBehandlingerISak(sak.id)
                                .map { it.toBehandlingSammendrag() }
                                .let { BehandlingListe(sak, it) }
                        }
                    call.respond(behandlinger)
                }
            }

            post("grunnlagsendringshendelser") {
                withFoedselsnummerInternal(tilgangService) { fnr ->
                    call.respond(
                        sakService.finnSaker(fnr.value).map { sak ->
                            GrunnlagsendringsListe(grunnlagsendringshendelseService.hentAlleHendelserForSak(sak.id))
                        }
                    )
                }
            }

            post("lukkgrunnlagsendringshendelse") {
                val lukketHendelse = call.receive<Grunnlagsendringshendelse>()
                grunnlagsendringshendelseService.lukkHendelseMedKommentar(hendelse = lukketHendelse)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}