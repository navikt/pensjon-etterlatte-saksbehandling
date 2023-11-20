package no.nav.etterlatte.libs.common.vedtak

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.Objects.isNull
import java.util.UUID

data class VedtakSammendragDto(
    val id: String,
    val behandlingId: UUID,
    val vedtakType: VedtakType?,
    val saksbehandlerId: String?,
    val datoFattet: ZonedDateTime?,
    val attestant: String?,
    val datoAttestert: ZonedDateTime?,
)

@Deprecated("VedtakDto skal ersttates av VedtakNyDto")
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

// TODO Rename til VedtakDto når gammel dto er faset ut
data class VedtakNyDto(
    val id: Long,
    val behandlingId: UUID,
    val status: VedtakStatus,
    val sak: VedtakSak,
    val type: VedtakType,
    val vedtakFattet: VedtakFattet?,
    val attestasjon: Attestasjon?,
    val innhold: VedtakInnholdDto,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class VedtakInnholdDto {
    @JsonTypeName("BEHANDLING")
    data class VedtakBehandlingDto(
        val virkningstidspunkt: YearMonth,
        val behandling: Behandling,
        val utbetalingsperioder: List<Utbetalingsperiode>,
    ) : VedtakInnholdDto()

    @JsonTypeName("TILBAKEKREVING")
    data class VedtakTilbakekrevingDto(
        val tilbakekreving: ObjectNode,
    ) : VedtakInnholdDto()
}

enum class VedtakStatus {
    OPPRETTET,
    FATTET_VEDTAK,
    ATTESTERT,
    TIL_SAMORDNING,
    SAMORDNET,
    RETURNERT,
    IVERKSATT,
}

data class Behandling(
    val type: BehandlingType,
    val id: UUID,
    val revurderingsaarsak: Revurderingaarsak? = null,
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
    val fnr: String,
    val status: VedtakStatus,
    val virkningstidspunkt: YearMonth,
    val sak: VedtakSak,
    val behandling: Behandling,
    val type: VedtakType,
    val vedtakFattet: VedtakFattet?,
    val attestasjon: Attestasjon?,
    val beregning: ObjectNode?,
    val avkorting: ObjectNode?,
    val utbetalingsperioder: List<Utbetalingsperiode>,
)

data class TilbakekrevingVedtakDto(
    val tilbakekrevingId: UUID,
    val sakId: Long,
    val sakType: SakType,
    val soeker: Folkeregisteridentifikator,
    val tilbakekreving: ObjectNode,
)

data class TilbakekrevingFattEllerAttesterVedtakDto(
    val tilbakekrevingId: UUID,
    val saksbehandler: String,
    val enhet: String,
)

data class TilbakekrevingVedtakLagretDto(
    val id: Long,
    val fattetAv: String,
    val enhet: String,
    val dato: LocalDate,
)
