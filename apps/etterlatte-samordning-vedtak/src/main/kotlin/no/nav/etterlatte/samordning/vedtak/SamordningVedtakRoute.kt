package no.nav.etterlatte.samordning.vedtak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.ktor.hentAccessToken
import java.time.LocalDate
import kotlin.random.Random

data class SamordningVedtakDto(
    val vedtakId: Long,
    val sakstype: String,
    val virkningsdato: LocalDate,
    val opphoersdato: LocalDate?,
    val type: SamordningVedtakType,
    val aarsak: String?,
    val anvendtTrygdetid: Int,
    val perioder: List<SamordningVedtakPeriode> = listOf()
)

data class SamordningVedtakPeriode(
    val fom: LocalDate,
    val tom: LocalDate? = null,
    val omstillingsstoenadBrutto: Int,
    val omstillingsstoenadNetto: Int
)

enum class SamordningVedtakType {
    START,
    ENDRING,
    OPPHOER
}

enum class SamordningVedtakAarsak {
    INNTEKT,
    ANNET
}

fun Route.samordningVedtakRoute(samordningVedtakService: SamordningVedtakService) {
    route("ping") {
        get {
            call.respond("Tjeneste ok")
        }
    }

    route("api/vedtak") {
        get("{vedtakId}") {
            val vedtakId = requireNotNull(call.parameters["vedtakId"]).toLong()

            // Trekk ut orgID for logging her og downstream
            val accessToken = hentAccessToken(call)

            val dto = samordningVedtakService.hentVedtak(vedtakId, accessToken)

            val dummystart = LocalDate.now().withDayOfMonth(1)
            val vedtaksinfo = dummySamordningVedtakDto(dummystart, vedtakId)

            call.respond(vedtaksinfo)
        }

        get {
            val virkFom =
                call.parameters["virkFom"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "virkFom ikke angitt")

            val fnr =
                call.request.headers["fnr"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "fnr ikke angitt")

            val vedtaksinfo = dummySamordningVedtakDto(virkFom, Random.nextLong())

            call.respond(listOf(vedtaksinfo))
        }
    }
}

private fun dummySamordningVedtakDto(
    virkFom: LocalDate,
    vedtakId: Long
) = SamordningVedtakDto(
    vedtakId = vedtakId,
    sakstype = "OMS",
    virkningsdato = virkFom,
    opphoersdato = null,
    type = SamordningVedtakType.START,
    aarsak = null,
    anvendtTrygdetid = 40,
    perioder =
        listOf(
            SamordningVedtakPeriode(
                fom = virkFom,
                omstillingsstoenadBrutto = 12000,
                omstillingsstoenadNetto = 9500
            )
        )
)