package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import no.nav.etterlatte.ExternalUser
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.libs.common.grunnlag.DetaljertGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("no.nav.etterlatte.grunnlag.GrunnlagRoutes")

data class SaksbehandlerOpplysning(val sakId: String, val behandlingId: String, val opplysning: List<Grunnlagsopplysning<ObjectNode>>)

//KUN LES
fun Route.grunnlagRoutes(service: GrunnlagService) {

    route("/behandlinger/{saksid}") {
        get {
            if (Kontekst.get()?.AppUser !is ExternalUser) {
                logger.warn("AppUser i kontekst er ikke ekstern bruker i endepunkt")
            }

            call.respond(service.hentGrunnlag(saksId.toLong())?.let {
                DetaljertGrunnlag(
                    it.saksId,
                    it.grunnlag,
                )
            } ?: HttpStatusCode.NotFound)
        }
    }

    post("/kommerbarnettilgode") {
        val body = call.receive<SaksbehandlerOpplysning>()
        service.opplysningFraSaksbehandler(body.sakId.toLong(), body.opplysning)
        call.respond("ok")
    }

}


inline val PipelineContext<*, ApplicationCall>.saksId
    get() = requireNotNull(call.parameters["saksid"])

inline val PipelineContext<*, ApplicationCall>.sakId get() = requireNotNull(call.parameters["sakid"]).toLong()

