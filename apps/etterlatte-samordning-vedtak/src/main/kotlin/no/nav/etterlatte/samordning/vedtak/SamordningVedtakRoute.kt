package no.nav.etterlatte.samordning.vedtak

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.time.LocalDate

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
    START, ENDRING, OPPHOER
}

enum class SamordningVedtakAarsak {
    INNTEKT, ANNET
}

fun Route.samordningVedtakRoute() {
    route("ping") {
        get {
            call.respond("Tjeneste ok")
        }
    }

    route("api/vedtak") {
        get("{vedtakId}") {
            val vedtakId = requireNotNull(call.parameters["vedtakId"]).toLong()

            val dummystart = LocalDate.now().withDayOfMonth(1)
            val vedtaksinfo = SamordningVedtakDto(
                vedtakId = vedtakId,
                sakstype = "OMS",
                virkningsdato = dummystart,
                opphoersdato = null,
                type = SamordningVedtakType.START,
                aarsak = null,
                anvendtTrygdetid = 40,
                perioder = listOf(
                    SamordningVedtakPeriode(
                        fom = dummystart,
                        omstillingsstoenadBrutto = 12000,
                        omstillingsstoenadNetto = 9500
                    )
                )
            )

            call.respond(vedtaksinfo)
        }
    }
}