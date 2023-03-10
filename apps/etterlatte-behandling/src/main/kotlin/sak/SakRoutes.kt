package no.nav.etterlatte.sak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.domain.toBehandlingSammendrag
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringsListe
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingListe
import no.nav.etterlatte.libs.common.behandling.SakType

internal fun Route.sakRoutes(
    sakService: SakService,
    generellBehandlingService: GenerellBehandlingService,
    grunnlagsendringshendelseService: GrunnlagsendringshendelseService
) {
    route("/saker") {
        get {
            call.respond(Saker(inTransaction { sakService.hentSaker() }))
        }

        get("/{id}") {
            val sak = inTransaction {
                sakService.finnSak(requireNotNull(call.parameters["id"]).toLong())
            }
            call.respond(sak ?: HttpStatusCode.NotFound)
        }
    }

    get("personer/{ident}/saker/{type}") {
        val ident = requireNotNull(call.parameters["ident"])
        val type: SakType = enumValueOf(requireNotNull(call.parameters["type"]))
        call.respond(inTransaction { sakService.finnEllerOpprettSak(ident, type) })
    }

    route("/api/personer/{fnr}") {
        get("behandlinger") {
            call.respond(
                sakService.finnSaker(requireNotNull(call.parameters["fnr"])).map { sak ->
                    generellBehandlingService.hentBehandlingerISak(sak.id).map {
                        it.toBehandlingSammendrag()
                    }.let { BehandlingListe(it) }
                }
            )
        }

        get("grunnlagsendringshendelser") {
            call.respond(
                sakService.finnSaker(requireNotNull(call.parameters["fnr"])).map { sak ->
                    GrunnlagsendringsListe(grunnlagsendringshendelseService.hentAlleHendelserForSak(sak.id))
                }
            )
        }
    }
}

data class Sak(val ident: String, val sakType: SakType, val id: Long)
data class Saker(val saker: List<Sak>)