package no.nav.etterlatte.sak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.inTransaction

fun Route.sakRoutes(sakService: SakService) {
    get("/saker") {
        call.respond(Saker(inTransaction { sakService.hentSaker() }))
    }

    get("/saker/{id}") {
        call.respond(inTransaction { sakService.finnSak(requireNotNull(call.parameters["id"]).toLong()) }
            ?: HttpStatusCode.NotFound)
    }
    delete("/saker/{id}/") {
        no.nav.etterlatte.sak.inTransaction { sakService.slettSak(requireNotNull(call.parameters["id"]).toLong()) }
        call.respond(HttpStatusCode.OK)
    }

    route("personer/{id}") {
        get("saker") {
            call.respond(Saker(inTransaction {
                sakService.finnSaker(requireNotNull(call.parameters["id"]))
            }))
        }

        route("saker/{type}") {
            get {
                val ident = requireNotNull(call.parameters["id"])
                val type = requireNotNull(call.parameters["type"])
                call.respond(inTransaction { sakService.finnEllerOpprettSak(ident, type) })
            }
        }
    }
}

private fun <T> inTransaction(block: () -> T): T = Kontekst.get().databasecontxt.inTransaction {
    block()
}

data class Sak(val ident: String, val sakType: String, val id: Long)
data class Saker(val saker: List<Sak>)



