package no.nav.etterlatte.libs.common.vedtak

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.math.BigDecimal
import java.time.YearMonth
import java.util.*
import java.util.Objects.isNull

data class VedtakDto(
    val vedtakId: Long,
    val virkningstidspunkt: YearMonth,
    val sak: Sak,
    val behandling: Behandling,
    val type: VedtakType,
    val vedtakFattet: VedtakFattet?,
    val attestasjon: Attestasjon?,
    val utbetalingsperioder: List<Utbetalingsperiode>
)

data class Behandling(
    val type: BehandlingType,
    val id: UUID
)

data class Periode(
    val fom: YearMonth,
    val tom: YearMonth?
) {
    init {
        require(isNull(tom) || fom == tom || fom.isBefore(tom))
    }
}

data class VedtakFattet(
    val ansvarligSaksbehandler: String,
    val ansvarligEnhet: String,
    val tidspunkt: Tidspunkt
)

data class Attestasjon(
    val attestant: String,
    val attesterendeEnhet: String,
    val tidspunkt: Tidspunkt
)

data class Utbetalingsperiode(
    val id: Long = 0L,
    val periode: Periode,
    val beloep: BigDecimal?,
    val type: UtbetalingsperiodeType
)

enum class UtbetalingsperiodeType {
    OPPHOER, UTBETALING
}

enum class VedtakType {
    INNVILGELSE, OPPHOER, AVSLAG, ENDRING
}