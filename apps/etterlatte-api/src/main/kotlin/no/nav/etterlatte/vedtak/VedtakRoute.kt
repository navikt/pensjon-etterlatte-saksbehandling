package no.nav.etterlatte.vedtak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondNullable
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.AuthorizationPlugin
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.ktor.token.Issuer
import java.math.BigDecimal
import java.time.YearMonth

fun Route.vedtakRoute(vedtakService: VedtakService) {
    // Tiltenkt for eksternt for etterlatte men internt i Nav. Initelt gjelder dette EESSI.
    route("api/v1/vedtak") {
        install(AuthorizationPlugin) {
            accessPolicyRolesEllerAdGrupper = setOf("les-bp-vedtak", "les-oms-vedtak")
            issuers = setOf(Issuer.AZURE.issuerName)
        }

        post {
            try {
                val request = call.receive<FoedselsnummerDTO>()
                val fnr = Folkeregisteridentifikator.of(request.foedselsnummer)
                val vedtak = vedtakService.hentVedtak(fnr)
                call.respond(vedtak)
            } catch (e: IllegalArgumentException) {
                call.respondNullable(HttpStatusCode.BadRequest, e.message)
            }
        }
    }
}

data class VedtakForEksterntDto(
    val vedtak: List<VedtakEksternt>,
)

data class VedtakEksternt(
    val sakId: Long,
    val sakType: String,
    val virkningstidspunkt: YearMonth,
    val type: VedtakTypeEksternt,
    val utbetaling: List<VedtakEksterntUtbetaling>,
)

enum class VedtakTypeEksternt {
    INNVILGELSE,
    OPPHOER,
    AVSLAG,
    ENDRING,
}

data class VedtakEksterntUtbetaling(
    val periode: Periode,
    val beloep: BigDecimal?,
)
