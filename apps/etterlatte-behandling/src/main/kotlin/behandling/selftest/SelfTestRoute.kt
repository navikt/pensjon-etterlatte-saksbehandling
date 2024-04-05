package no.nav.etterlatte.behandling.selftest

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

internal fun Route.selfTestRoute(selfTestService: SelfTestService) {
    get("internal/selftest") {
        val accept = call.request.header(HttpHeaders.Accept)
        if (accept == null) {
            call.respond(HttpStatusCode.BadRequest)
        } else {
            if (accept.contains(ContentType.Application.Json.contentType)) {
                call.respond(selfTestService.performSelfTestAndReportAsJson())
            } else if (accept == ContentType.Text.Html.contentType) {
                call.respondText(selfTestService.performSelfTestAndReportAsHtml(), ContentType.Text.Html, HttpStatusCode.OK)
            }
        }
    }
}
