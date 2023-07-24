package no.nav.etterlatte.libs.common

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.token.Saksbehandler
import no.nav.etterlatte.token.Systembruker
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.util.*

const val BEHANDLINGSID_CALL_PARAMETER = "behandlingsid"
const val SAKID_CALL_PARAMETER = "sakId"

inline val PipelineContext<*, ApplicationCall>.behandlingsId: UUID
    get() = call.parameters[BEHANDLINGSID_CALL_PARAMETER]?.let { UUID.fromString(it) } ?: throw NullPointerException(
        "BehandlingsId er ikke i path params"
    )

inline val PipelineContext<*, ApplicationCall>.sakId: Long
    get() = call.parameters[SAKID_CALL_PARAMETER]?.toLong() ?: throw NullPointerException(
        "SakId er ikke i path params"
    )

suspend inline fun PipelineContext<*, ApplicationCall>.withBehandlingId(
    behandlingTilgangsSjekk: BehandlingTilgangsSjekk,
    onSuccess: (id: UUID) -> Unit
) = withParam(BEHANDLINGSID_CALL_PARAMETER) { behandlingId ->
    when (brukerTokenInfo) {
        is Saksbehandler -> {
            val harTilgangTilBehandling =
                behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, brukerTokenInfo as Saksbehandler)
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
    when (brukerTokenInfo) {
        is Saksbehandler -> {
            val harTilgangTilSak = sakTilgangsSjekk.harTilgangTilSak(sakId, brukerTokenInfo as Saksbehandler)
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
    personTilgangsSjekk: PersonTilgangsSjekk,
    onSuccess: (fnr: Folkeregisteridentifikator) -> Unit
) {
    val foedselsnummerDTO = call.receive<FoedselsnummerDTO>()
    val foedselsnummer = Folkeregisteridentifikator.of(foedselsnummerDTO.foedselsnummer)
    when (brukerTokenInfo) {
        is Saksbehandler -> {
            val harTilgangTilPerson = personTilgangsSjekk.harTilgangTilPerson(
                foedselsnummer,
                brukerTokenInfo as Saksbehandler
            )
            if (harTilgangTilPerson) {
                onSuccess(foedselsnummer)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        else -> onSuccess(foedselsnummer)
    }
}

suspend inline fun <reified T : Any> PipelineContext<*, ApplicationCall>.medBody(onSuccess: (t: T) -> Unit) {
    try {
        val body = call.receive<T>()
        onSuccess(body)
    } catch (e: Exception) {
        call.respond(HttpStatusCode.BadRequest, "Feil under deserialiseringen av objektet")
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.kunSystembruker(
    onSuccess: () -> Unit
) {
    when (brukerTokenInfo) {
        is Systembruker -> {
            onSuccess()
        }
        else -> call.respond(HttpStatusCode.NotFound)
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.kunSaksbehandler(
    onSuccess: () -> Unit
) {
    when (brukerTokenInfo) {
        is Saksbehandler -> {
            onSuccess()
        }
        else -> call.respond(HttpStatusCode.Forbidden)
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

suspend inline fun PipelineContext<*, ApplicationCall>.hentNavidentFraToken(
    onSuccess: (navident: String) -> Unit
) {
    val navident = call.principal<TokenValidationContextPrincipal>()
        ?.context?.firstValidToken?.get()?.jwtTokenClaims?.get("NAVident")?.toString()
    if (navident.isNullOrEmpty()) {
        call.respond(
            HttpStatusCode.Unauthorized,
            "Kunne ikke hente ut navident "
        )
    } else {
        onSuccess(navident)
    }
}

fun ApplicationCall.uuid(param: String) = this.parameters[param]?.let {
    UUID.fromString(it)
} ?: throw NullPointerException(
    "$param er ikke i path params"
)

interface IFoedselsnummerDTO {
    val foedselsnummer: String
}
data class FoedselsnummerDTO(
    override val foedselsnummer: String
) : IFoedselsnummerDTO

data class FoedselsNummerMedGraderingDTO(
    override val foedselsnummer: String,
    val gradering: AdressebeskyttelseGradering? = null
) : IFoedselsnummerDTO

interface BehandlingTilgangsSjekk {
    suspend fun harTilgangTilBehandling(behandlingId: UUID, bruker: Saksbehandler): Boolean
}

interface SakTilgangsSjekk {
    suspend fun harTilgangTilSak(sakId: Long, bruker: Saksbehandler): Boolean
}

interface PersonTilgangsSjekk {
    suspend fun harTilgangTilPerson(foedselsnummer: Folkeregisteridentifikator, bruker: Saksbehandler): Boolean
}