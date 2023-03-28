package no.nav.etterlatte.libs.common

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.ktor.bruker
import no.nav.etterlatte.token.Saksbehandler
import no.nav.etterlatte.token.SystemBruker
import java.util.*

const val BEHANDLINGSID_CALL_PARAMETER = "behandlingsid"
const val SAKID_CALL_PARAMETER = "sakId"
const val FNR_CALL_PARAMETER = "fnr"

inline val PipelineContext<*, ApplicationCall>.behandlingsId: UUID
    get() = call.parameters[BEHANDLINGSID_CALL_PARAMETER]?.let { UUID.fromString(it) } ?: throw NullPointerException(
        "BehandlingsId er ikke i path params"
    )

// Denne skal vi unngå å bruke, skal legges i request body med en post
inline val PipelineContext<*, ApplicationCall>.fnr: String
    get() = call.parameters[FNR_CALL_PARAMETER] ?: throw NullPointerException(
        "Fnr er ikke i path params"
    )

inline val PipelineContext<*, ApplicationCall>.sakId: Long
    get() = call.parameters[SAKID_CALL_PARAMETER]?.toLong() ?: throw NullPointerException(
        "SakId er ikke i path params"
    )

suspend inline fun PipelineContext<*, ApplicationCall>.withBehandlingId(
    behandlingTilgangsSjekk: BehandlingTilgangsSjekk,
    onSuccess: (id: UUID) -> Unit
) = withParam(BEHANDLINGSID_CALL_PARAMETER) { behandlingId ->
    when (bruker) {
        is Saksbehandler -> {
            val harTilgangTilBehandling =
                behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, bruker as Saksbehandler)
            if (harTilgangTilBehandling) {
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
            val harTilgangTilSak = sakTilgangsSjekk.harTilgangTilSak(sakId, bruker as Saksbehandler)
            if (harTilgangTilSak) {
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
            val harTilgangTilPerson = personTilgangsSjekk.harTilgangTilPerson(foedselsnummer, bruker as Saksbehandler)
            if (harTilgangTilPerson) {
                onSuccess(foedselsnummer)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        else -> onSuccess(foedselsnummer)
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.kunSystembruker(
    onSuccess: () -> Unit
) {
    when (bruker) {
        is SystemBruker -> {
            onSuccess()
        }
        else -> call.respond(HttpStatusCode.NotFound)
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

data class FoedselsnummerDTO(
    val foedselsnummer: String
)

interface BehandlingTilgangsSjekk {
    suspend fun harTilgangTilBehandling(behandlingId: UUID, bruker: Saksbehandler): Boolean
}

interface SakTilgangsSjekk {
    suspend fun harTilgangTilSak(sakId: Long, bruker: Saksbehandler): Boolean
}

interface PersonTilgangsSjekk {
    suspend fun harTilgangTilPerson(foedselsnummer: Foedselsnummer, bruker: Saksbehandler): Boolean
}