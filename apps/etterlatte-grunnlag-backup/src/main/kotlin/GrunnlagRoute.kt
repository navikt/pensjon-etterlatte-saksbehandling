package no.nav.etterlatte

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.helse.rapids_rivers.toUUID
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

fun Route.grunnlagRoute(dao: OpplysningDao) {
    get("/grunnlag/{opplysning_id}") {
        val opplysningId = call.parameters["opplysning_id"]!!.toUUID()

        measureTimedValue {
            dao.hent(opplysningId)
        }.let { (hendelse, varighet) ->
            logger.info("Hentet hendelse med id $opplysningId tok: ${varighet.toString(DurationUnit.SECONDS, 2)}")

            call.respond(hendelse)
        }
    }
}
