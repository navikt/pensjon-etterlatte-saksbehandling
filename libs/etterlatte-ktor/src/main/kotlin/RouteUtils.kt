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
        val uuidParam = try {
            UUID.fromString(value)
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, "$param må være UUID")
            return
        }
        onSuccess(uuidParam)
    } else {
        call.respond(HttpStatusCode.BadRequest, "$param var null, forventet en UUID")
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
        val (uuidParam1, uuidParam2) = try {
            UUID.fromString(value1) to UUID.fromString(value2)
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, "$param1 og $param2 må være UUID")
            return
        }
        onSuccess(uuidParam1, uuidParam2)
    } else {
        call.respond(
            HttpStatusCode.BadRequest,
            "$param1 eller $param2 var null, men de må være UUID " +
                "(fikk param1=$value1 og param2=$value2)"
        )
    }
}