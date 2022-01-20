package no.nav.etterlatte.sak

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.etterlatte.Kontekst


fun Route.sakRoutes(sakService: SakService){
    get("/saker") {
        call.respond(inTransaction { sakService.hentSaker() })
    }

    get("/saker/{id}") {
        call.respond(inTransaction { sakService.finnSak(requireNotNull(call.parameters["id"]).toLong()) } ?: HttpStatusCode.NotFound)
    }
    route("personer/{id}"){
        get("saker") {
            call.respond(Person(inTransaction {
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

data class Person(val saker: List<Sak>)

data class Sak(val ident: String, val sakType: String, val id:Long)



