package no.nav.etterlatte.libs.ktor.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

const val BEHANDLINGID_CALL_PARAMETER = "behandlingId"
const val SAKID_CALL_PARAMETER = "sakId"
const val OPPGAVEID_CALL_PARAMETER = "oppgaveId"
const val KLAGEID_CALL_PARAMETER = "klageId"
const val GENERELLBEHANDLINGID_CALL_PARAMETER = "generellBehandlingId"
const val TILBAKEKREVINGID_CALL_PARAMETER = "tilbakekrevingId"

enum class CallParamAuthId(
    val value: String,
) {
    BEHANDLINGID(BEHANDLINGID_CALL_PARAMETER),
    SAKID(SAKID_CALL_PARAMETER),
    OPPGAVEID(OPPGAVEID_CALL_PARAMETER),
    KLAGEID(KLAGEID_CALL_PARAMETER),
    GENERELLBEHANDLINGID(GENERELLBEHANDLINGID_CALL_PARAMETER),
    TILBAKEKREVINGID(TILBAKEKREVINGID_CALL_PARAMETER),
}

const val OPPGAVEID_GOSYS_CALL_PARAMETER = "gosysOppgaveId"

inline val PipelineContext<*, ApplicationCall>.generellBehandlingId: UUID
    get() =
        call.parameters[GENERELLBEHANDLINGID_CALL_PARAMETER]?.let { UUID.fromString(it) } ?: throw NullPointerException(
            "GenerellBehandlingId er ikke i path params",
        )

inline val PipelineContext<*, ApplicationCall>.behandlingId: UUID
    get() =
        call.parameters[BEHANDLINGID_CALL_PARAMETER]?.let { UUID.fromString(it) } ?: throw NullPointerException(
            "BehandlingId er ikke i path params",
        )

inline val PipelineContext<*, ApplicationCall>.sakId: SakId
    get() =
        call.parameters[SAKID_CALL_PARAMETER]?.toLong() ?: throw NullPointerException(
            "SakId er ikke i path params",
        )

inline val PipelineContext<*, ApplicationCall>.oppgaveId: UUID
    get() =
        requireNotNull(call.parameters[OPPGAVEID_CALL_PARAMETER]?.let { UUID.fromString(it) }) {
            "OppgaveId er ikke i path params"
        }

inline val PipelineContext<*, ApplicationCall>.klageId: UUID
    get() =
        requireNotNull(call.parameters[KLAGEID_CALL_PARAMETER]?.let { UUID.fromString(it) }) {
            "KlageId er ikke i path params"
        }

inline val PipelineContext<*, ApplicationCall>.gosysOppgaveId: String
    get() =
        requireNotNull(call.parameters[OPPGAVEID_GOSYS_CALL_PARAMETER]) {
            "Gosys oppgaveId er ikke i path params"
        }

inline val PipelineContext<*, ApplicationCall>.tilbakekrevingId: UUID
    get() =
        call.parameters[TILBAKEKREVINGID_CALL_PARAMETER]?.let { UUID.fromString(it) } ?: throw NullPointerException(
            "TilbakekrevingId er ikke i path params",
        )

val logger = LoggerFactory.getLogger("TilgangsSjekk")

suspend inline fun PipelineContext<*, ApplicationCall>.withBehandlingId(
    behandlingTilgangsSjekk: BehandlingTilgangsSjekk,
    skrivetilgang: Boolean = false,
    onSuccess: (id: UUID) -> Unit,
) = withUuidParam(BEHANDLINGID_CALL_PARAMETER) { behandlingId ->
    when (brukerTokenInfo) {
        is Saksbehandler -> {
            val harTilgangTilBehandling =
                behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, skrivetilgang, brukerTokenInfo as Saksbehandler)
            if (harTilgangTilBehandling) {
                onSuccess(behandlingId)
            } else {
                logger.info("Har ikke tilgang til behandling")
                throw GenerellIkkeFunnetException()
            }
        }

        else -> onSuccess(behandlingId)
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.withSakId(
    sakTilgangsSjekk: SakTilgangsSjekk,
    skrivetilgang: Boolean = false,
    onSuccess: (id: SakId) -> Unit,
) {
    val sakId =
        try {
            call.parameters[SAKID_CALL_PARAMETER]?.toLong()
        } catch (e: Exception) {
            throw UgyldigForespoerselException("SAKID_IKKE_TALL", "Kunne ikke lese ut sakId-parameter")
        }
    if (sakId == null) {
        throw UgyldigForespoerselException("SAKID_MANGLER", "Mangler påkrevd sakId som parameter")
    }
    when (val token = brukerTokenInfo) {
        is Saksbehandler -> {
            val harTilgangTilSak =
                sakTilgangsSjekk.harTilgangTilSak(sakId, skrivetilgang, token)
            if (harTilgangTilSak) {
                onSuccess(sakId)
            } else {
                logger.info("Har ikke tilgang til sak")
                throw GenerellIkkeFunnetException()
            }
        }

        is Systembruker -> onSuccess(sakId)
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.withFoedselsnummer(
    personTilgangsSjekk: PersonTilgangsSjekk,
    skrivetilgang: Boolean = false,
    onSuccess: (fnr: Folkeregisteridentifikator) -> Unit,
) {
    val foedselsnummerDTO = call.receive<FoedselsnummerDTO>()
    val foedselsnummer = Folkeregisteridentifikator.of(foedselsnummerDTO.foedselsnummer)
    when (brukerTokenInfo) {
        is Saksbehandler -> {
            val harTilgangTilPerson =
                personTilgangsSjekk.harTilgangTilPerson(
                    foedselsnummer,
                    skrivetilgang,
                    brukerTokenInfo as Saksbehandler,
                )
            if (harTilgangTilPerson) {
                onSuccess(foedselsnummer)
            } else {
                logger.info("Har ikke tilgang til person")
                throw GenerellIkkeFunnetException()
            }
        }

        else -> onSuccess(foedselsnummer)
    }
}

suspend inline fun <reified T : Any> PipelineContext<*, ApplicationCall>.medBody(onSuccess: (t: T) -> Unit) {
    val body =
        try {
            call.receive<T>()
        } catch (e: Exception) {
            sikkerlogger().error("Feil under deserialisering", e)
            call.respond(HttpStatusCode.BadRequest, "Feil under deserialiseringen av objektet")
            return
        }
    onSuccess(body)
}

inline fun PipelineContext<*, ApplicationCall>.kunSystembruker(onSuccess: (systemBruker: Systembruker) -> Unit) {
    when (val bruker = brukerTokenInfo) {
        is Systembruker -> {
            onSuccess(bruker)
        }

        else -> {
            logger.debug("Endepunktet er ikke tilgjengeliggjort for saksbehandler, avviser forespørselen")
            throw GenerellIkkeFunnetException()
        }
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.kunSaksbehandler(onSuccess: (Saksbehandler) -> Unit) {
    when (val token = brukerTokenInfo) {
        is Saksbehandler -> {
            onSuccess(token)
        }

        else -> {
            logger.debug("Endepunktet er ikke tilgjengeliggjort for systembruker, avviser forespørselen")
            call.respond(HttpStatusCode.Forbidden)
        }
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.withUuidParam(
    param: String,
    onSuccess: (value: UUID) -> Unit,
) {
    val value = call.parameters[param]
    if (value != null) {
        val uuidParam =
            try {
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

fun ApplicationCall.uuid(param: String) =
    this.parameters[param]?.let {
        UUID.fromString(it)
    } ?: throw NullPointerException(
        "$param er ikke i path params",
    )

class UgyldigDatoFormatException :
    UgyldigForespoerselException(
        code = "UGYLDIG-DATOFORMAT",
        detail = "Forventet format YYYY-MM-DD (ISO-8601)",
    )

fun ApplicationCall.dato(param: String) =
    this.parameters[param]?.let {
        runCatching { LocalDate.parse(it) }
            .getOrElse { throw UgyldigDatoFormatException() }
    }

suspend fun PipelineContext<Unit, ApplicationCall>.hvisEnabled(
    featureToggleService: FeatureToggleService,
    toggle: FeatureToggle,
    block: suspend PipelineContext<Unit, ApplicationCall>.() -> Unit,
) {
    if (featureToggleService.isEnabled(toggle, false)) {
        block()
    } else {
        throw FeatureIkkeStoettetException()
    }
}

class FeatureIkkeStoettetException :
    ForespoerselException(
        code = "NOT_IMPLEMENTED",
        status = HttpStatusCode.NotImplemented.value,
        detail = "Funksjonaliteten er ikke tilgjengelig enda.",
    )

fun BrukerTokenInfo.lagGrunnlagsopplysning() =
    if (this is Saksbehandler) {
        Grunnlagsopplysning.Saksbehandler.create(ident())
    } else {
        Grunnlagsopplysning.Gjenny.create(ident())
    }

val routeLogger = LoggerFactory.getLogger("Route")
