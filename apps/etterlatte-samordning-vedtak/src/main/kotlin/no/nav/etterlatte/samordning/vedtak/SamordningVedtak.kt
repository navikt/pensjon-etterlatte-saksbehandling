package no.nav.etterlatte.samordning.vedtak

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import java.time.LocalDate
import java.time.YearMonth

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SamordningVedtakDto(
    val vedtakId: Long,
    val sakstype: String,
    val virkningsdato: LocalDate,
    val opphoersdato: LocalDate?,
    val type: SamordningVedtakType,
    val aarsak: String?,
    val anvendtTrygdetid: Int,
    val perioder: List<SamordningVedtakPeriode> = listOf(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SamordningVedtakPeriode(
    val fom: LocalDate,
    val tom: LocalDate? = null,
    val omstillingsstoenadBrutto: Int,
    val omstillingsstoenadNetto: Int,
)

enum class SamordningVedtakType {
    START,
    ENDRING,
    OPPHOER,
}

enum class SamordningVedtakAarsak {
    INNTEKT,
    ANNET,
}

fun YearMonth.atStartOfMonth(): LocalDate = this.atDay(1)

class VedtakFeilSakstypeException : UgyldigForespoerselException(
    code = "002-FEIL_SAKSTYPE",
    detail = "Forespurt informasjon gjeldende ikke-støttet sakstype",
)

class ManglerTpNrException : UgyldigForespoerselException(
    code = "001-TPNR-MANGLER",
    detail = "Forespørselen mangler 'tpnr' header",
)
