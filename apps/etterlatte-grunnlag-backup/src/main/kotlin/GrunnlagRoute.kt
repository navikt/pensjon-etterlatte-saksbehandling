package no.nav.etterlatte

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.helse.rapids_rivers.toUUID
import java.util.UUID
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

    post("/grunnlag/hendelser") {
        val hendelseIder = call.receive<HendelseIdWrapper>()

        measureTimedValue {
            dao.hentBulk(hendelseIder.ider)
        }.let { (hendelser, varighet) ->
            logger.info("Henting ${hendelser.size} hendelser tok: ${varighet.toString(DurationUnit.SECONDS, 2)}")

            call.respond(HendelseResponse(hendelser))
        }
    }
}

data class HendelseIdWrapper(
    val ider: List<UUID>,
)

data class HendelseResponse(
    val hendelser: List<OpplysningDao.GrunnlagHendelse>,
)
