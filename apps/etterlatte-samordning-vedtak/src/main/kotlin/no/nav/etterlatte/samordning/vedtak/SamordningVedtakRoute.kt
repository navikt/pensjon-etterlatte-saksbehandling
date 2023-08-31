package no.nav.etterlatte.samordning.vedtak

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.behandling.SakType
import java.time.LocalDate

data class SamordningVedtakDto(
    val vedtakId: Long,
    val sakstype: String,
    val virkningsdato: LocalDate,
    val opphorsdato: LocalDate?,
    val resultatkode: String,
    val stoppaarsak: String?,
    val anvendtTrygdetid: Int,
    val omstillingsstoenadBrutto: Int,
    val omstillingsstoenadNetto: Int
)

fun Route.samordningVedtakRoute() {
    route("ping") {
        get {
            call.respond("Tjeneste ok")
        }
    }

    route("api/vedtak") {
        get("{vedtakId}") {
            val vedtakId = requireNotNull(call.parameters["vedtakId"]).toLong()

            val vedtaksinfo = SamordningVedtakDto(
                vedtakId = vedtakId,
                sakstype = SakType.OMSTILLINGSSTOENAD.name,
                virkningsdato = LocalDate.now(),
                opphorsdato = null,
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