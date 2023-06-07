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
import no.nav.etterlatte.libs.common.withFoedselsnummer
import no.nav.etterlatte.libs.common.withFoedselsnummerAndGradering

internal fun Route.sakRoutes(
    tilgangService: TilgangService,
    sakService: SakService,
    generellBehandlingService: GenerellBehandlingService,
    grunnlagsendringshendelseService: GrunnlagsendringshendelseService
) {
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
                val sakId = call.parameters["sakId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Mangler sakId"
                )
                when (val sisteIverksatteBehandling = generellBehandlingService.hentSisteIverksatte(sakId.toLong())) {
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
        withFoedselsnummer(tilgangService) { fnr ->
            val type: SakType = enumValueOf(requireNotNull(call.parameters["type"]))
            val sak = inTransaction { sakService.finnSak(fnr.value, type) }
            call.respond(sak ?: HttpStatusCode.NotFound)
        }
    }

    route("/api/personer/") {
        post("behandlinger") {
            withFoedselsnummer(tilgangService) { fnr ->
                val behandlinger = sakService.finnSaker(fnr.value)
                    .map { sak ->
                        generellBehandlingService.hentBehandlingerISak(sak.id).map {
                            it.toBehandlingSammendrag()
                        }.let { BehandlingListe(it) }
                    }
                call.respond(behandlinger)
            }
        }

        post("grunnlagsendringshendelser") {
            withFoedselsnummer(tilgangService) { fnr ->
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

    route("/api/sak/{$SAKID_CALL_PARAMETER}") {
        get {
            withSakIdInternal(tilgangService) { sakId ->
                val sak = inTransaction {
                    sakService.finnSak(sakId)
                }
                call.respond(sak?.ident ?: HttpStatusCode.NotFound)
            }
        }
    }
}