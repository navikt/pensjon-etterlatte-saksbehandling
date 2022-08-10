package no.nav.etterlatte.grunnlag

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper

fun Route.grunnlagRoute(service: GrunnlagService) {
    route("grunnlag") {
        get("{sakId}/{opplysningType}") {
            val grunnlag = service.hentGrunnlagAvType(
                call.parameters["sakId"]!!.toLong(),
                Opplysningstyper.valueOf(call.parameters["opplysningType"].toString())
            )

            if (grunnlag != null) {
                call.respond(grunnlag)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
