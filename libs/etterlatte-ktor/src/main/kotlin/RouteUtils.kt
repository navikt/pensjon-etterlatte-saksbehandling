package no.nav.etterlatte.libs.common

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.libs.ktor.ADGroup
import no.nav.etterlatte.libs.ktor.bruker
import no.nav.etterlatte.libs.ktor.harGruppetilgang
import no.nav.etterlatte.token.Saksbehandler
import no.nav.etterlatte.token.System
import java.util.*

const val BEHANDLINGSID_CALL_PARAMETER = "behandlingsid"

inline val PipelineContext<*, ApplicationCall>.behandlingsId: UUID
    get() = call.parameters[BEHANDLINGSID_CALL_PARAMETER]?.let { UUID.fromString(it) } ?: throw NullPointerException(
        "BehandlingsId er ikke i path params"
    )

suspend inline fun PipelineContext<*, ApplicationCall>.withBehandlingId(onSuccess: (id: UUID) -> Unit) =
    withParam(BEHANDLINGSID_CALL_PARAMETER, onSuccess)

suspend inline fun PipelineContext<*, ApplicationCall>.withLesetilgang(onSuccess: (id: UUID) -> Unit) {
    withBehandlingId { behandlingId ->
        when (bruker) {
            is Saksbehandler -> {
                // todo: Oppslag mot behandling for å sjekke tilgang (adressebeskyttelse, skjerming).
                onSuccess(behandlingId)
            }
            is System -> onSuccess(behandlingId)
        }
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.withSaksbehandlertilgang(onSuccess: (id: UUID) -> Unit) {
    withBehandlingId { behandlingId ->
        when (bruker) {
            is Saksbehandler -> {
                if (harGruppetilgang(ADGroup.SAKSBEHANDLER)) {
                    // todo: Oppslag mot behandling for å sjekke tilgang (adressebeskyttelse, skjerming).
                    onSuccess(behandlingId)
                }

                call.respond(HttpStatusCode.Forbidden)
            }
            is System -> onSuccess(behandlingId)
        }
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.withParam(
    param: String,
    onSuccess: (value: UUID) -> Unit
) {
    val value = call.parameters[param]
    if (value != null) {
        val uuidParam = try {
            UUID.fromString(value)
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, "$param må være UUID, fikk $value")
            return
        }
        onSuccess(uuidParam)
    } else {
        call.respond(HttpStatusCode.BadRequest, "$param var null, forventet en UUID")
    }
}