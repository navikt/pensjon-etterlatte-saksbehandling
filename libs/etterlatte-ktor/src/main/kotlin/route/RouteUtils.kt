package no.nav.etterlatte.libs.ktor.route

import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.tilSakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

const val BEHANDLINGID_CALL_PARAMETER = "behandlingId"
const val SAKID_CALL_PARAMETER = "sakId"
const val OPPGAVEID_CALL_PARAMETER = "oppgaveId"
const val KLAGEID_CALL_PARAMETER = "klageId"
const val GENERELLBEHANDLINGID_CALL_PARAMETER = "generellBehandlingId"
const val TILBAKEKREVINGID_CALL_PARAMETER = "tilbakekrevingId"
const val FORBEHANDLINGID_CALL_PARAMETER = "etteroppgjoerId"

enum class CallParamAuthId(
    val value: String,
) {
    BEHANDLINGID(BEHANDLINGID_CALL_PARAMETER),
    SAKID(SAKID_CALL_PARAMETER),
    OPPGAVEID(OPPGAVEID_CALL_PARAMETER),
    KLAGEID(KLAGEID_CALL_PARAMETER),
    GENERELLBEHANDLINGID(GENERELLBEHANDLINGID_CALL_PARAMETER),
    TILBAKEKREVINGID(TILBAKEKREVINGID_CALL_PARAMETER),
    ETTEROPPGJOERID(FORBEHANDLINGID_CALL_PARAMETER),
}

const val OPPGAVEID_GOSYS_CALL_PARAMETER = "gosysOppgaveId"

inline val RoutingContext.generellBehandlingId: UUID
    get() =
        call.parameters[GENERELLBEHANDLINGID_CALL_PARAMETER]?.let { UUID.fromString(it) } ?: throw NullPointerException(
            "GenerellBehandlingId er ikke i path params",
        )

inline val RoutingContext.behandlingId: UUID
    get() =
        call.parameters.parseUuidParameter(BEHANDLINGID_CALL_PARAMETER)

inline val RoutingContext.sakId: SakId
    get() =
        try {
            call.parameters[SAKID_CALL_PARAMETER]?.tilSakId()!!
        } catch (e: Exception) {
            when (call.parameters[SAKID_CALL_PARAMETER]) {
                null -> throw UgyldigForespoerselException("MANGLER_SAKID", "SakId er ikke i path parameters")
                else -> throw UgyldigForespoerselException("IKKE_GYLDIG_SAKID", "SakId er ikke gyldig")
            }
        }

inline val RoutingContext.oppgaveId: UUID
    get() =
        call.parameters.parseUuidParameter(OPPGAVEID_CALL_PARAMETER)

inline val RoutingContext.klageId: UUID
    get() =
        call.parameters.parseUuidParameter(KLAGEID_CALL_PARAMETER)

inline val RoutingContext.gosysOppgaveId: String
    get() =
        krevIkkeNull(call.parameters[OPPGAVEID_GOSYS_CALL_PARAMETER]) {
            "Gosys oppgaveId er ikke i path params"
        }

inline val RoutingContext.tilbakekrevingId: UUID
    get() = call.parameters.parseUuidParameter(TILBAKEKREVINGID_CALL_PARAMETER)

inline val RoutingContext.forbehandlingId: UUID
    get() =
        call.parameters.parseUuidParameter(FORBEHANDLINGID_CALL_PARAMETER)

val logger: Logger = LoggerFactory.getLogger("TilgangsSjekk")

suspend inline fun RoutingContext.withBehandlingId(
    behandlingTilgangsSjekk: BehandlingTilgangsSjekk,
    skrivetilgang: Boolean = false,
    onSuccess: (id: UUID) -> Unit,
) = withUuidParam(BEHANDLINGID_CALL_PARAMETER) { behandlingId ->
    withBehandlingId(behandlingId, behandlingTilgangsSjekk, skrivetilgang, onSuccess)
}

suspend inline fun RoutingContext.withSakId(
    sakTilgangsSjekk: SakTilgangsSjekk,
    skrivetilgang: Boolean = false,
    onSuccess: (id: SakId) -> Unit,
) {
    val sakId =
        try {
            call.parameters[SAKID_CALL_PARAMETER]?.tilSakId()
        } catch (_: Exception) {
            throw UgyldigForespoerselException("SAKID_IKKE_TALL", "Kunne ikke lese ut sakId-parameter")
        }
    if (sakId == null) {
        throw UgyldigForespoerselException("SAKID_MANGLER", "Mangler påkrevd sakId som parameter")
    }
    withSakId(sakId, sakTilgangsSjekk, skrivetilgang, onSuccess)
}

fun Parameters.parseUuidParameter(parameter: String): UUID =
    try {
        this[parameter]?.let { UUID.fromString(it) }
            ?: throw UgyldigForespoerselException("PARAMETER_MANGLER", "$parameter er ikke i path params.")
    } catch (e: Exception) {
        throw UgyldigForespoerselException(
            "PARAMETER_UGYLDIG",
            "$parameter er ikke en gyldig id. " +
                "Hvis du har navigert til denne siden er det en kobling som er ødelagt i Gjenny. " +
                "Beskriv hvilke steg du fulgte og meld sak i porten.",
            cause = e,
        )
    }

suspend inline fun RoutingContext.withSakId(
    sakId: SakId,
    sakTilgangsSjekk: SakTilgangsSjekk,
    skrivetilgang: Boolean,
    onSuccess: (id: SakId) -> Unit,
) {
    when (val token = brukerTokenInfo) {
        is Saksbehandler -> {
            val harTilgangTilSak =
                sakTilgangsSjekk.harTilgangTilSak(sakId, skrivetilgang, token)
            if (harTilgangTilSak) {
                onSuccess(sakId)
            } else {
                logger.info("Har ikke tilgang til sak")
                throw IkkeTilgangTilSakException()
            }
        }

        is Systembruker -> {
            onSuccess(sakId)
        }
    }
}

suspend inline fun RoutingContext.withBehandlingId(
    behandlingId: UUID,
    behandlingTilgangsSjekk: BehandlingTilgangsSjekk,
    skrivetilgang: Boolean,
    onSuccess: (id: UUID) -> Unit,
) {
    when (val bruker = brukerTokenInfo) {
        is Saksbehandler -> {
            val harTilgangTilBehandling =
                behandlingTilgangsSjekk.harTilgangTilBehandling(
                    behandlingId,
                    skrivetilgang,
                    bruker,
                )
            if (harTilgangTilBehandling) {
                onSuccess(behandlingId)
            } else {
                logger.info("Har ikke tilgang til behandling")
                throw IkkeTilgangTilBehandlingException()
            }
        }

        else -> {
            onSuccess(behandlingId)
        }
    }
}

suspend inline fun RoutingContext.withFoedselsnummer(
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

        else -> {
            onSuccess(foedselsnummer)
        }
    }
}

suspend inline fun <reified T : Any> RoutingContext.medBody(onSuccess: (t: T) -> Unit) {
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

inline fun RoutingContext.kunSystembruker(onSuccess: (systemBruker: Systembruker) -> Unit) {
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

suspend inline fun RoutingContext.kunSaksbehandler(onSuccess: (Saksbehandler) -> Unit) {
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

suspend inline fun RoutingContext.withUuidParam(
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

class FeatureIkkeStoettetException :
    ForespoerselException(
        code = "NOT_IMPLEMENTED",
        status = HttpStatusCode.NotImplemented.value,
        detail = "Funksjonaliteten er ikke tilgjengelig enda.",
    )

class IkkeTilgangTilBehandlingException :
    ForespoerselException(
        code = "IKKE_TILGANG_TIL_BEHANDLING",
        status = HttpStatusCode.Forbidden.value,
        detail = "Bruker har ikke tilgang til behandling",
    )

class IkkeTilgangTilSakException :
    ForespoerselException(
        code = "IKKE_TILGANG_TIL_SAK",
        status = HttpStatusCode.Forbidden.value,
        detail = "Bruker har ikke tilgang til sak",
    )

fun BrukerTokenInfo.lagGrunnlagsopplysning() =
    if (this is Saksbehandler) {
        Grunnlagsopplysning.Saksbehandler.create(ident())
    } else {
        Grunnlagsopplysning.Gjenny.create(ident())
    }
