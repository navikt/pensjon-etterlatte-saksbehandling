package no.nav.etterlatte.sak

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.inTransaction

fun Route.sakRoutes(sakService: SakService){
    get("/saker") {
        call.respond(Saker(inTransaction { sakService.hentSaker() }))
    }

    get("/saker/{id}") {
        call.respond(inTransaction { sakService.finnSak(requireNotNull(call.parameters["id"]).toLong()) } ?: HttpStatusCode.NotFound)
    }
    delete("/saker/{id}/") {
        sakService.slettSak(requireNotNull(call.parameters["id"]).toLong())
        call.respond(inTransaction { sakService.slettSak(requireNotNull(call.parameters["id"]).toLong()) })
    }
    route("personer/{id}"){
        get("saker") {
            call.respond(Saker(inTransaction {
                sakService.finnSaker(requireNotNull(call.parameters["id"]))
            }))
        }

        route("saker/{type}"){
            get {
                val ident = requireNotNull(call.parameters["id"])
                val type = requireNotNull(call.parameters["type"])
                call.respond(inTransaction { sakService.finnEllerOpprettSak(ident, type) })
            }
        }
    }
}

private fun <T> inTransaction(block:()->T): T = Kontekst.get().databasecontxt.inTransaction {
    block()
}

data class Sak(val ident: String, val sakType: String, val id:Long)
data class Saker(val saker: List<Sak>)



