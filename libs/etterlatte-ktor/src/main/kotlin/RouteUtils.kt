package no.nav.etterlatte.libs.common

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import java.util.*

suspend inline fun PipelineContext<*, ApplicationCall>.withBehandlingId(onSuccess: (id: UUID) -> Unit) =
    withParam("behandlingId", onSuccess)

suspend inline fun PipelineContext<*, ApplicationCall>.withParam(
    param: String,
    onSuccess: (value: UUID) -> Unit
) {
    val value = call.parameters[param]
    if (value != null) {
        try {
            onSuccess(UUID.fromString(value))
        } catch (e: IllegalArgumentException) {
            // TODO dette er skummelt - fanger opp alle IllegalArgumentException som skjer i onSuccess
            call.respond(HttpStatusCode.BadRequest, "$param må være UUID")
        }
    } else {
        call.respond(HttpStatusCode.BadRequest, "$param var null")
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.withParam(
    param1: String,
    param2: String,
    onSuccess: (value1: UUID, value2: UUID) -> Unit
) {
    val value1 = call.parameters[param1]
    val value2 = call.parameters[param2]
    if (value1 != null && value2 != null) {
        try {
            onSuccess(UUID.fromString(value1), UUID.fromString(value2))
        } catch (e: IllegalArgumentException) {
            // TODO dette er skummelt - fanger opp alle IllegalArgumentException som skjer i onSuccess
            call.respond(HttpStatusCode.BadRequest, "$param1 og $param2 må være UUID")
        }
    } else {
        call.respond(HttpStatusCode.BadRequest, "$param1 eller $param2 var null")
    }
}