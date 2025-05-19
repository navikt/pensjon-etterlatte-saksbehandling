package no.nav.etterlatte.vedtaksvurdering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondNullable
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
import no.nav.etterlatte.rapidsandrivers.Behandlingssteg
import org.slf4j.LoggerFactory

fun Route.dollyRoute(
    vedtakService: VedtaksvurderingService,
    dollyService: DollyService,
) {
    route("/dolly") {
        val logger = LoggerFactory.getLogger("DollyRoute")

        post("api/v1/opprett-ytelse") {
            try {
                val nySoeknadRequest = call.receive<NySoeknadRequest>()
                val ytelse = nySoeknadRequest.type
                val behandlingssteg = Behandlingssteg.IVERKSATT
                val gjenlevende = nySoeknadRequest.gjenlevende
                val avdoed = nySoeknadRequest.avdoed
                val barnListe = nySoeknadRequest.barn
                var soeker = nySoeknadRequest.soeker
                if (soeker == "") {
                    soeker =
                        when (ytelse) {
                            SoeknadType.BARNEPENSJON -> barnListe.first()
                            SoeknadType.OMSTILLINGSSTOENAD -> gjenlevende
                        }
                }
                if (soeker == "" || avdoed == "") {
                    call.respond(HttpStatusCode.BadRequest, "Påkrevde felter mangler")
                }
                val request =
                    NySoeknadRequest(
                        ytelse,
                        avdoed,
                        gjenlevende,
                        barnListe,
                        soeker = soeker,
                    )

                val brukerId = brukerTokenInfo.ident()
                dollyService.sendSoeknadFraDolly(request, brukerId, behandlingssteg)
                call.respond(HttpStatusCode.Created)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Noe gikk galt")
            }
        }
        post("api/v1/hent-ytelse") {
            try {
                val request = call.receive<FoedselsnummerDTO>()
                val fnr = haandterUgyldigIdent(request.foedselsnummer)
                val vedtak = vedtakService.hentVedtak(fnr)
                call.respond(vedtak)
            } catch (e: UgyldigFoedselsnummerException) {
                call.respondNullable(HttpStatusCode.BadRequest, e.detail)
            } catch (e: IllegalArgumentException) {
                call.respondNullable(HttpStatusCode.BadRequest, e.message)
            }
        }
    }
}

fun haandterUgyldigIdent(fnr: String): Folkeregisteridentifikator {
    try {
        return Folkeregisteridentifikator.of(fnr)
    } catch (_: Exception) {
        throw UgyldigFoedselsnummerException()
    }
}

data class NySoeknadRequest(
    val type: SoeknadType,
    val avdoed: String,
    val gjenlevende: String,
    val barn: List<String> = emptyList(),
    val soeker: String? = null,
)

enum class VedtakType {
    INNVILGELSE,
    OPPHOER,
    AVSLAG,
    ENDRING,
}

class UgyldigFoedselsnummerException :
    UgyldigForespoerselException(
        code = "006-FNR-UGYLDIG",
        detail = "Ugyldig fødselsnummer",
        meta = getMeta(),
    )

fun getMeta() =
    mapOf(
        "correlation-id" to getCorrelationId(),
        "tidspunkt" to Tidspunkt.now(),
    )
