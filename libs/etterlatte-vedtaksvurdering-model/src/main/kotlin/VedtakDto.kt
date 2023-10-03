package no.nav.etterlatte.libs.common.vedtak

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.math.BigDecimal
import java.time.YearMonth
import java.util.Objects.isNull
import java.util.UUID

data class VedtakDto(
    val vedtakId: Long,
    val status: VedtakStatus,
    val virkningstidspunkt: YearMonth,
    val sak: VedtakSak,
    val behandling: Behandling,
    val type: VedtakType,
    val vedtakFattet: VedtakFattet?,
    val attestasjon: Attestasjon?,
    val utbetalingsperioder: List<Utbetalingsperiode>,
)

enum class VedtakStatus {
    OPPRETTET,
    FATTET_VEDTAK,
    ATTESTERT,
    RETURNERT,
    IVERKSATT,
}

data class Behandling(
    val type: BehandlingType,
    val id: UUID,
    val revurderingsaarsak: RevurderingAarsak? = null,
    val revurderingInfo: RevurderingInfo? = null,
)

data class Periode(
    val fom: YearMonth,
    val tom: YearMonth?,
) {
    init {
        require(isNull(tom) || fom == tom || fom.isBefore(tom))
    }
}

data class VedtakFattet(
    val ansvarligSaksbehandler: String,
    val ansvarligEnhet: String,
    val tidspunkt: Tidspunkt,
)

data class Attestasjon(
    val attestant: String,
    val attesterendeEnhet: String,
    val tidspunkt: Tidspunkt,
)

data class AttesterVedtakDto(val kommentar: String)

data class Utbetalingsperiode(
    val id: Long? = null,
    val periode: Periode,
    val beloep: BigDecimal?,
    val type: UtbetalingsperiodeType,
)

enum class UtbetalingsperiodeType {
    OPPHOER,
    UTBETALING,
}

data class VedtakSamordningDto(
    val vedtakId: Long,
    val status: VedtakStatus,
    val virkningstidspunkt: YearMonth,
    val sak: VedtakSak,
    val behandling: Behandling,
    val type: VedtakType,
    val vedtakFattet: VedtakFattet?,
    val attestasjon: Attestasjon?,
    val beregning: ObjectNode?,
    val avkorting: ObjectNode?,
)

data class TilbakekrevingVedtakDto(
    val tilbakekrevingId: UUID,
    val sakId: Long,
    val sakType: SakType,
    val soeker: Folkeregisteridentifikator,
    val fattet: VedtakFattet,
    val tilbakekreving: ObjectNode,
)
