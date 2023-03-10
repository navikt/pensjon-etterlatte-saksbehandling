package no.nav.etterlatte.libs.common

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.ktor.bruker
import no.nav.etterlatte.token.Saksbehandler
import java.util.*

const val BEHANDLINGSID_CALL_PARAMETER = "behandlingsid"
const val SAKID_CALL_PARAMETER = "sakId"

inline val PipelineContext<*, ApplicationCall>.behandlingsId: UUID
    get() = call.parameters[BEHANDLINGSID_CALL_PARAMETER]?.let { UUID.fromString(it) } ?: throw NullPointerException(
        "BehandlingsId er ikke i path params"
    )

suspend inline fun PipelineContext<*, ApplicationCall>.withBehandlingId(
    behandlingTilgangsSjekk: BehandlingTilgangsSjekk,
    onSuccess: (id: UUID) -> Unit
) = withParam(BEHANDLINGSID_CALL_PARAMETER) { behandlingId ->
    when (bruker) {
        is Saksbehandler -> {
            if (behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, bruker as Saksbehandler)) {
                onSuccess(behandlingId)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        else -> onSuccess(behandlingId)
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.withSakId(
    sakTilgangsSjekk: SakTilgangsSjekk,
    onSuccess: (id: Long) -> Unit
) = call.parameters[SAKID_CALL_PARAMETER]!!.toLong().let { sakId ->
    when (bruker) {
        is Saksbehandler -> {
            if (sakTilgangsSjekk.harTilgangTilSak(sakId, bruker as Saksbehandler)) {
                onSuccess(sakId)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        else -> onSuccess(sakId)
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.withFoedselsnummer(
    fnr: String,
    personTilgangsSjekk: PersonTilgangsSjekk,
    onSuccess: (fnr: Foedselsnummer) -> Unit
) = Foedselsnummer.of(fnr).let { foedselsnummer ->
    when (bruker) {
        is Saksbehandler -> {
            if (personTilgangsSjekk.harTilgangTilPerson(foedselsnummer, bruker as Saksbehandler)) {
                onSuccess(foedselsnummer)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        else -> onSuccess(foedselsnummer)
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

interface BehandlingTilgangsSjekk {
    suspend fun harTilgangTilBehandling(behandlingId: UUID, bruker: Saksbehandler): Boolean
}

interface SakTilgangsSjekk {
    suspend fun harTilgangTilSak(sakId: Long, bruker: Saksbehandler): Boolean
}

interface PersonTilgangsSjekk {
    suspend fun harTilgangTilPerson(foedselsnummer: Foedselsnummer, bruker: Saksbehandler): Boolean
}