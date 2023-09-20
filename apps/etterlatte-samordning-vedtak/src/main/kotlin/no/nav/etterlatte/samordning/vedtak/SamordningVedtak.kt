package no.nav.etterlatte.samordning.vedtak

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

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

class VedtakFeilSakstypeException : RuntimeException()
