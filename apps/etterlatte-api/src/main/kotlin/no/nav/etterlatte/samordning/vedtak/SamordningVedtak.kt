package no.nav.etterlatte.samordning.vedtak

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.logging.getCorrelationId
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
    DOEDSFALL,
    REGULERING,
    ANNET,
}

fun YearMonth.atStartOfMonth(): LocalDate = this.atDay(1)

class VedtakFeilSakstypeException :
    UgyldigForespoerselException(
        code = "004-FEIL_SAKSTYPE",
        detail = "Forespurt informasjon gjeldende ikke-støttet sakstype",
        meta = getMeta(),
    )

class ManglerTpNrException :
    UgyldigForespoerselException(
        code = "001-TPNR-MANGLER",
        detail = "Forespørselen mangler 'tpnr' header",
        meta = getMeta(),
    )

class ManglerFoedselsnummerException :
    UgyldigForespoerselException(
        code = "002-FNR-MANGLER",
        detail = "fnr ikke angitt",
        meta = getMeta(),
    )

class ManglerFomDatoException :
    UgyldigForespoerselException(
        code = "003-FOMDATO-MANGLER",
        detail = "fomDato ikke angitt",
        meta = getMeta(),
    )

class ManglerPaaDatoException :
    UgyldigForespoerselException(
        code = "005-PAAMDATO-MANGLER",
        detail = "paaDato ikke angitt",
        meta = getMeta(),
    )

fun getMeta() =
    mapOf(
        "correlation-id" to getCorrelationId(),
        "tidspunkt" to Tidspunkt.now(),
    )
