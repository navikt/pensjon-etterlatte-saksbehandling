package no.nav.etterlatte

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.helse.rapids_rivers.toUUID

fun Route.grunnlagRoute(dao: OpplysningDao) {
    get("/grunnlag/{opplysning_id}") {
        val hendelse = dao.hent(call.parameters["opplysning_id"]!!.toUUID())

        call.respond(hendelse)
    }
}
