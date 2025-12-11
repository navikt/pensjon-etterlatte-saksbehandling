package no.nav.etterlatte.libs.common.vedtak

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.Regelverk
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
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
    val behandlendeSaksbehandler: String?,
    val datoFattet: ZonedDateTime?,
    val attesterendeSaksbehandler: String?,
    val datoAttestert: ZonedDateTime?,
    val virkningstidspunkt: YearMonth?,
    val opphoerFraOgMed: YearMonth?,
    val iverksettelsesTidspunkt: Tidspunkt? = null,
)

data class VedtakDto(
    val id: Long,
    val behandlingId: UUID,
    val status: VedtakStatus,
    val sak: VedtakSak,
    val type: VedtakType,
    val vedtakFattet: VedtakFattet?,
    val attestasjon: Attestasjon?,
    val innhold: VedtakInnholdDto,
    val iverksettelsesTidspunkt: Tidspunkt? = null,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class VedtakInnholdDto {
    @JsonTypeName("BEHANDLING")
    data class VedtakBehandlingDto(
        val virkningstidspunkt: YearMonth,
        val behandling: Behandling,
        val utbetalingsperioder: List<Utbetalingsperiode>,
        val opphoerFraOgMed: YearMonth?,
    ) : VedtakInnholdDto()

    @JsonTypeName("TILBAKEKREVING")
    data class VedtakTilbakekrevingDto(
        val tilbakekreving: ObjectNode,
    ) : VedtakInnholdDto()

    @JsonTypeName("KLAGE")
    data class Klage(
        val klage: ObjectNode,
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
)

data class Periode(
    val fom: YearMonth,
    val tom: YearMonth?,
) {
    init {
        krev(isNull(tom) || fom == tom || fom.isBefore(tom)) {
            "Fom må vera før eller lik tom, men fom er $fom og tom er $tom"
        }
    }
}

data class VedtakFattet(
    val ansvarligSaksbehandler: String,
    val ansvarligEnhet: Enhetsnummer,
    val tidspunkt: Tidspunkt,
)

data class Attestasjon(
    val attestant: String,
    val attesterendeEnhet: Enhetsnummer,
    val tidspunkt: Tidspunkt,
)

data class AttesterVedtakDto(
    val kommentar: String,
)

data class Utbetalingsperiode(
    val id: Long? = null,
    val periode: Periode,
    val beloep: BigDecimal?,
    val type: UtbetalingsperiodeType,
    val regelverk: Regelverk,
)

enum class UtbetalingsperiodeType {
    OPPHOER,
    UTBETALING,
}

data class AvkortetYtelsePeriode(
    val id: UUID,
    val vedtakId: Long,
    val fom: YearMonth,
    val tom: YearMonth?,
    val type: String,
    val ytelseFoerAvkorting: Int,
    val ytelseEtterAvkorting: Int,
)

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
    val perioder: List<VedtakSamordningPeriode>,
)

data class VedtakSamordningPeriode(
    val fom: YearMonth,
    val tom: YearMonth?,
    val ytelseFoerAvkorting: Int,
    val ytelseEtterAvkorting: Int,
)

data class VedtakEtteroppgjoerDto(
    val vedtakId: Long,
    val perioder: List<VedtakEtteroppgjoerPeriode>,
)

data class VedtakslisteEtteroppgjoerRequest(
    val sakId: SakId,
    val etteroppgjoersAar: Int,
)

data class VedtakEtteroppgjoerPeriode(
    val fom: YearMonth,
    val tom: YearMonth?,
    val ytelseEtterAvkorting: Int,
)

data class TilbakekrevingVedtakDto(
    val tilbakekrevingId: UUID,
    val sakId: SakId,
    val sakType: SakType,
    val soeker: Folkeregisteridentifikator,
    val tilbakekreving: ObjectNode,
)

data class TilbakekrevingFattEllerAttesterVedtakDto(
    val tilbakekrevingId: UUID,
    val enhet: Enhetsnummer,
)

data class TilbakekrevingVedtakLagretDto(
    val id: Long,
    val fattetAv: String,
    val enhet: Enhetsnummer,
    val dato: LocalDate,
)

data class InnvilgetPeriodeDto(
    val periode: Periode,
    val vedtak: List<VedtakDto>,
)
