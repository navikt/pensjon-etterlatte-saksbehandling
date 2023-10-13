package no.nav.etterlatte.vedtaksvurdering

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakNyDto
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class OpprettVedtak(
    val soeker: Folkeregisteridentifikator,
    val sakId: Long,
    val sakType: SakType,
    val behandlingId: UUID,
    val status: VedtakStatus = VedtakStatus.OPPRETTET,
    val type: VedtakType,
    val innhold: VedtakInnhold,
)

data class Vedtak(
    val id: Long,
    val soeker: Folkeregisteridentifikator,
    val sakId: Long,
    val sakType: SakType,
    val behandlingId: UUID,
    val status: VedtakStatus,
    val type: VedtakType,
    val vedtakFattet: VedtakFattet? = null,
    val attestasjon: Attestasjon? = null,
    val innhold: VedtakInnhold,
) {
    @Deprecated("VedtakDto skal ersttates av VedtakNyDto")
    fun toDto(): VedtakDto {
        val innhold = innhold as VedtakBehandlingInnhold
        return VedtakDto(
            vedtakId = id,
            status = status,
            sak = VedtakSak(soeker.value, sakType, sakId),
            type = type,
            vedtakFattet = vedtakFattet,
            attestasjon = attestasjon,
            virkningstidspunkt = innhold.virkningstidspunkt,
            behandling =
                Behandling(
                    innhold.behandlingType,
                    behandlingId,
                    innhold.revurderingAarsak,
                    innhold.revurderingInfo,
                ),
            utbetalingsperioder = innhold.utbetalingsperioder,
        )
    }

    fun toNyDto(): VedtakNyDto {
        return VedtakNyDto(
            id = id,
            behandlingId = behandlingId,
            status = status,
            sak = VedtakSak(soeker.value, sakType, sakId),
            type = type,
            vedtakFattet = vedtakFattet,
            attestasjon = attestasjon,
            innhold =
                when (innhold) {
                    is VedtakBehandlingInnhold ->
                        VedtakInnholdDto.VedtakBehandlingDto(
                            virkningstidspunkt = innhold.virkningstidspunkt,
                            behandling =
                                Behandling(
                                    innhold.behandlingType,
                                    behandlingId,
                                    innhold.revurderingAarsak,
                                    innhold.revurderingInfo,
                                ),
                            utbetalingsperioder = innhold.utbetalingsperioder,
                        )

                    is VedtakTilbakekrevingInnhold ->
                        VedtakInnholdDto.VedtakTilbakekrevingDto(
                            tilbakekreving = innhold.tilbakekreving,
                        )
                },
        )
    }
}

sealed interface VedtakInnhold

data class VedtakBehandlingInnhold(
    val behandlingType: BehandlingType,
    val revurderingAarsak: RevurderingAarsak?,
    val virkningstidspunkt: YearMonth,
    val beregning: ObjectNode?,
    val avkorting: ObjectNode?,
    val vilkaarsvurdering: ObjectNode?,
    val utbetalingsperioder: List<Utbetalingsperiode>,
    val revurderingInfo: RevurderingInfo? = null,
) : VedtakInnhold

data class VedtakTilbakekrevingInnhold(
    val tilbakekreving: ObjectNode,
) : VedtakInnhold

data class LoependeYtelse(val erLoepende: Boolean, val dato: LocalDate)
