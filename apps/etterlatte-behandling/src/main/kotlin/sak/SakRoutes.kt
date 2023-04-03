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
import no.nav.etterlatte.behandling.domain.toBehandlingSammendrag
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringsListe
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.FoedselsnummerDTO
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.kunSystembruker
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.libs.common.withFoedselsnummer

internal fun Route.sakRoutes(
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

        get("/{$SAKID_CALL_PARAMETER}") {
            val sak = inTransaction {
                sakService.finnSak(sakId)
            }
            call.respond(sak ?: HttpStatusCode.NotFound)
        }
    }

    post("personer/saker/{type}") {
        val foedselsnummerDTO = call.receive<FoedselsnummerDTO>()
        val fnr = foedselsnummerDTO.foedselsnummer
        withFoedselsnummer(fnr, sakService) {
            val type: SakType = enumValueOf(requireNotNull(call.parameters["type"]))
            call.respond(inTransaction { sakService.finnEllerOpprettSak(fnr, type) })
        }
    }

    route("/api/personer/") {
        post("behandlinger") {
            val foedselsnummerDTO = call.receive<FoedselsnummerDTO>()
            val fnr = foedselsnummerDTO.foedselsnummer
            withFoedselsnummer(fnr, sakService) {
                val behandlinger = sakService.finnSaker(fnr)
                    .map { sak ->
                        generellBehandlingService.hentBehandlingerISak(sak.id).map {
                            it.toBehandlingSammendrag()
                        }.let { BehandlingListe(it) }
                    }
                call.respond(behandlinger)
            }
        }

        post("grunnlagsendringshendelser") {
            val foedselsnummerDTO = call.receive<FoedselsnummerDTO>()
            val fnr = foedselsnummerDTO.foedselsnummer
            withFoedselsnummer(fnr, sakService) {
                call.respond(
                    sakService.finnSaker(fnr).map { sak ->
                        GrunnlagsendringsListe(grunnlagsendringshendelseService.hentAlleHendelserForSak(sak.id))
                    }
                )
            }
        }
    }
}

data class Sak(val ident: String, val sakType: SakType, val id: Long, val enhet: String? = null)

data class Saker(val saker: List<Sak>)