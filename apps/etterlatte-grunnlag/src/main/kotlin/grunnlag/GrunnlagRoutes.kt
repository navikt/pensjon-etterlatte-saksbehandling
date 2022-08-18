package no.nav.etterlatte.grunnlag

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
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