package no.nav.etterlatte.samordning.vedtak

import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.time.LocalDate

data class SamordningVedtakDto(
    val vedtakId: Long,
    val sakstype: String,
    val virkningsdato: LocalDate,
    val resultatkode: String,
    val stoppaarsak: String?,
    val anvendtTrygdetid: Int,
    val omstillingsstoenadBrutto: Int,
    val omstillingsstoenadNetto: Int
)

fun Route.vedtakRoute() {
    val logger = application.log

    route("ping") {
        get {
            call.respond("Tjeneste ok")
        }
    }

    route("vedtak") {
        get("{vedtakId}") {
            val vedtakId = requireNotNull(call.parameters["vedtakId"]).toLong()

            val vedtaksinfo = SamordningVedtakDto(
                vedtakId = vedtakId,
                sakstype = "OMSTILLINGSSTOENAD",
                virkningsdato = LocalDate.now(),
                resultatkode = "RESULTATKODE",
                stoppaarsak = null,
                anvendtTrygdetid = 40,
                omstillingsstoenadBrutto = 12000,
                omstillingsstoenadNetto = 9500
            )

            call.respond(vedtaksinfo)
        }
    }
}