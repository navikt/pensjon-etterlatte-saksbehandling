package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.libs.common.vedtak.Behandling
import java.math.BigDecimal
import java.time.YearMonth

data class Utbetalingsvedtak(
    val vedtakId: Long,
    val sak: Sak,
    val behandling: Behandling,
    val pensjonTilUtbetaling: List<Utbetalingsperiode>,
    val vedtakFattet: VedtakFattet,
    val attestasjon: Attestasjon
)

data class Sak(val ident: String, val id: Long)

data class Attestasjon(val attestant: String)

data class Utbetalingsperiode(
    val id: Long,
    val periode: Periode,
    val beloep: BigDecimal?,
    val type: UtbetalingsperiodeType
)

data class VedtakFattet(val ansvarligSaksbehandler: String)
data class Periode(
    val fom: YearMonth,
    val tom: YearMonth?
)

enum class UtbetalingsperiodeType {
    OPPHOER, UTBETALING // TODO: trenger vi en ENDRING-type her?
}