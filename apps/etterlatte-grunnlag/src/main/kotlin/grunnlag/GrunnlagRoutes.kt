package no.nav.etterlatte.grunnlag

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import no.nav.etterlatte.libs.common.grunnlag.DetaljertGrunnlag
import java.util.*


//KUN LES
fun Route.grunnlagRoutes(service: GrunnlagService) {

  //Kun denne vi skal beholde? Og endre
    route("/behandlinger/{saksid}") {
        get {
            //TODO toLong?!?
            call.respond(service.hentGrunnlag(saksId.toLong())?.let {
                DetaljertGrunnlag(
                    it.id,
                    it.saksId,
                    it.grunnlag,
                )
            } ?: HttpStatusCode.NotFound)
        }
    }
}


inline val PipelineContext<*, ApplicationCall>.saksId
    get() = requireNotNull(call.parameters["saksid"])

        /*.let {
        UUID.fromString(
            it
        )
    }

         */
inline val PipelineContext<*, ApplicationCall>.sakId get() = requireNotNull(call.parameters["sakid"]).toLong()

