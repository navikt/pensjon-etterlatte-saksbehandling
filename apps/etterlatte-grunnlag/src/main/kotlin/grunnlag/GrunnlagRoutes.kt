package no.nav.etterlatte.grunnlag

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import no.nav.etterlatte.ExternalUser
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.libs.common.grunnlag.DetaljertGrunnlag
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("no.nav.etterlatte.grunnlag.GrunnlagRoutes")

//KUN LES
fun Route.grunnlagRoutes(service: GrunnlagService) {

  //TODO Kun denne vi skal beholde? Og endre
    route("/behandlinger/{saksid}") {
        get {
            if(Kontekst.get()?.AppUser !is ExternalUser){ logger.warn("AppUser i kontekst er ikke ekstern bruker i endepunkt") }

            //TODO toLong?!?
            call.respond(service.hentGrunnlag(saksId.toLong())?.let {
                DetaljertGrunnlag(
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

