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
    val behandlingType: BehandlingType,
    val virkningstidspunkt: YearMonth,
    val status: VedtakStatus = VedtakStatus.OPPRETTET,
    val type: VedtakType,
    val beregning: ObjectNode?,
    val avkorting: ObjectNode?,
    val vilkaarsvurdering: ObjectNode?,
    val utbetalingsperioder: List<Utbetalingsperiode>,
    val revurderingsaarsak: RevurderingAarsak?,
    val revurderingInfo: RevurderingInfo?,
)

sealed class VedtakFelles(
    open val id: Long,
    open val soeker: Folkeregisteridentifikator,
    open val sakId: Long,
    open val sakType: SakType,
    open val behandlingId: UUID,
    open val status: VedtakStatus,
    open val type: VedtakType,
    open val vedtakFattet: VedtakFattet? = null,
    open val attestasjon: Attestasjon? = null,
)

data class VedtakSammendrag(
    override val id: Long,
    override val soeker: Folkeregisteridentifikator,
    override val sakId: Long,
    override val sakType: SakType,
    override val behandlingId: UUID,
    override val status: VedtakStatus,
    override val type: VedtakType,
    override val vedtakFattet: VedtakFattet? = null,
    override val attestasjon: Attestasjon? = null,
) : VedtakFelles(
        id = id,
        soeker = soeker,
        sakId = sakId,
        sakType = sakType,
        behandlingId = behandlingId,
        status = status,
        type = type,
        vedtakFattet = vedtakFattet,
        attestasjon = attestasjon,
    )

data class Vedtak(
    override val id: Long,
    override val soeker: Folkeregisteridentifikator,
    override val sakId: Long,
    override val sakType: SakType,
    override val behandlingId: UUID,
    val behandlingType: BehandlingType,
    val revurderingAarsak: RevurderingAarsak?,
    val virkningstidspunkt: YearMonth,
    override val status: VedtakStatus,
    override val type: VedtakType,
    val beregning: ObjectNode?,
    val avkorting: ObjectNode?,
    val vilkaarsvurdering: ObjectNode?,
    val utbetalingsperioder: List<Utbetalingsperiode>,
    override val vedtakFattet: VedtakFattet? = null,
    override val attestasjon: Attestasjon? = null,
    val revurderingInfo: RevurderingInfo? = null,
) : VedtakFelles(
        id = id,
        soeker = soeker,
        sakId = sakId,
        sakType = sakType,
        behandlingId = behandlingId,
        status = status,
        type = type,
        vedtakFattet = vedtakFattet,
        attestasjon = attestasjon,
    ) {
    fun toDto() =
        VedtakDto(
            vedtakId = id,
            status = status,
            virkningstidspunkt = virkningstidspunkt,
            sak = VedtakSak(soeker.value, sakType, sakId),
            behandling = Behandling(behandlingType, behandlingId, revurderingAarsak, revurderingInfo),
            type = type,
            utbetalingsperioder = utbetalingsperioder,
            vedtakFattet = vedtakFattet,
            attestasjon = attestasjon,
        )
}

data class TilbakekrevingsVedtak(
    override val id: Long,
    override val soeker: Folkeregisteridentifikator,
    override val sakId: Long,
    override val sakType: SakType,
    override val behandlingId: UUID,
    override val status: VedtakStatus,
    override val type: VedtakType,
    override val vedtakFattet: VedtakFattet? = null,
    override val attestasjon: Attestasjon? = null,
    val tilbakekreving: ObjectNode,
) : VedtakFelles(
        id = id,
        soeker = soeker,
        sakId = sakId,
        sakType = sakType,
        behandlingId = behandlingId,
        status = status,
        type = type,
        vedtakFattet = vedtakFattet,
        attestasjon = attestasjon,
    )

data class LoependeYtelse(val erLoepende: Boolean, val dato: LocalDate)
