package no.nav.etterlatte.vedtaksvurdering

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

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
    val vilkaarsvurdering: ObjectNode?,
    val utbetalingsperioder: List<Utbetalingsperiode>
)

data class Vedtak(
    val id: Long,
    val soeker: Folkeregisteridentifikator,
    val sakId: Long,
    val sakType: SakType,
    val behandlingId: UUID,
    val behandlingType: BehandlingType,
    val virkningstidspunkt: YearMonth,
    val status: VedtakStatus,
    val type: VedtakType,
    val beregning: ObjectNode?,
    val vilkaarsvurdering: ObjectNode?,
    val utbetalingsperioder: List<Utbetalingsperiode>,
    val vedtakFattet: VedtakFattet? = null,
    val attestasjon: Attestasjon? = null
) {
    fun toDto() = VedtakDto(
        vedtakId = id,
        status = status,
        virkningstidspunkt = virkningstidspunkt,
        sak = VedtakSak(soeker.value, sakType, sakId),
        behandling = Behandling(behandlingType, behandlingId),
        type = type,
        utbetalingsperioder = utbetalingsperioder,
        vedtakFattet = vedtakFattet,
        attestasjon = attestasjon
    )
}

data class VedtakHendelse(
    val vedtakId: Long,
    val inntruffet: Tidspunkt,
    val saksbehandler: String? = null,
    val kommentar: String? = null,
    val valgtBegrunnelse: String? = null
)

data class LoependeYtelse(val erLoepende: Boolean, val dato: LocalDate)