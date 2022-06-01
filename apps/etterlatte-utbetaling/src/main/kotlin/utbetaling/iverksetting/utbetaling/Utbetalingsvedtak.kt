package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import java.math.BigDecimal
import java.time.YearMonth
import java.util.*

data class Utbetalingsvedtak(
    val vedtakId: Long,
    val sak: Sak,
    val behandling: Behandling,
    val pensjonTilUtbetaling: List<Utbetalingsperiode>,
    val vedtakFattet: VedtakFattet,
    val attestasjon: Attestasjon,
)

data class Sak(val ident: String, val id: Long)

data class Behandling(val type: BehandlingType, val id: UUID)

data class Attestasjon(val attestant: String)

data class Utbetalingsperiode(
    val id: Long, val periode: Periode, val beloep: BigDecimal?, val type: UtbetalingsperiodeType
)

data class VedtakFattet(val ansvarligSaksbehandler: String)

enum class BehandlingType { REVURDERING, FORSTEGANGSBEHANDLING }

data class Periode(
    val fom: YearMonth, val tom: YearMonth?
)

enum class UtbetalingsperiodeType {
    OPPHOER, UTBETALING  // TODO: trenger vi en ENDRING-type her?
}