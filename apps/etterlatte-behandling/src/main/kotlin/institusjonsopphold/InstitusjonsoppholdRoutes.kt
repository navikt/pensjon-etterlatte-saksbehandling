package no.nav.etterlatte.institusjonsopphold

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.sakId

internal fun Route.institusjonsoppholdRoute(institusjonsoppholdService: InstitusjonsoppholdService) {
    route("/api/institusjonsoppholdbegrunnelse/{$SAKID_CALL_PARAMETER}") {
        post {
            val institusjonsoppholdBegrunnelse = call.receive<InstitusjonsoppholdBegrunnelse>()
            institusjonsoppholdService.leggInnInstitusjonsoppholdBegrunnelse(sakId, institusjonsoppholdBegrunnelse)
            call.respond(HttpStatusCode.OK)
        }
    }
}