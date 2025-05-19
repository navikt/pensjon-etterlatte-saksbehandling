package no.nav.etterlatte.vedtaksvurdering.dollybehandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingService

fun Route.dollyRoute(
    vedtakService: VedtaksvurderingService,
    dollyService: DollyService,
) {
    route("/dolly") {
        post("api/v1/opprett-ytelse") {
            try {
                val nySoeknadRequest = call.receive<NySoeknadRequest>()
                val brukerId = brukerTokenInfo.ident()
                dollyService.sendSoeknadFraDolly(nySoeknadRequest, brukerId)
                call.respond(HttpStatusCode.Created)
            } catch (e: ContentTransformationException) {
                throw DollyExceptions.UgyldigRequest()
            }
        }
        post("api/v1/hent-ytelse") {
            try {
                val request = call.receive<FoedselsnummerDTO>()
                val fnr = request.foedselsnummer.tilFolkeregisterIdent()
                val vedtak = vedtakService.hentVedtak(fnr)
                call.respond(vedtak)
            } catch (e: IllegalArgumentException) {
                throw DollyExceptions.UgyldigParameter(e)
            }
        }
    }
}

private fun String.tilFolkeregisterIdent(): Folkeregisteridentifikator {
    try {
        return Folkeregisteridentifikator.of(this)
    } catch (_: Exception) {
        throw DollyExceptions.UgyldigFoedselsnummerException()
    }
}

data class NySoeknadRequest(
    val type: SoeknadType,
    val avdoed: String,
    val gjenlevende: String,
    val barn: List<String> = emptyList(),
    var soeker: String? = null,
) {
    init {
        if (soeker == "") {
            soeker =
                when (type) {
                    SoeknadType.BARNEPENSJON -> barn.firstOrNull()
                    SoeknadType.OMSTILLINGSSTOENAD -> gjenlevende
                }
        }
        if (soeker.isNullOrBlank() || avdoed.isBlank()) {
            throw DollyExceptions.ManglerFelter()
        }
    }
}

object DollyExceptions {
    private fun getMeta() =
        mapOf(
            "correlation-id" to getCorrelationId(),
            "tidspunkt" to Tidspunkt.now(),
        )

    class UgyldigParameter(
        cause: Exception,
    ) : UgyldigForespoerselException("BAD_REQUEST", "Kunne ikke lese forespørselen.", cause = cause, meta = getMeta())

    class UgyldigRequest :
        UgyldigForespoerselException(
            "BAD_REQUEST",
            "Kunne ikke lese ut en NySoeknadRequest fra body.",
            meta = getMeta(),
        )

    class ManglerFelter : UgyldigForespoerselException("MANGLER_FELTER", "Påkrevde felter mangler.", meta = getMeta())

    class UgyldigFoedselsnummerException :
        UgyldigForespoerselException(
            code = "006-FNR-UGYLDIG",
            detail = "Ugyldig fødselsnummer",
            meta = getMeta(),
        )
}
