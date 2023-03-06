package no.nav.etterlatte.vedtaksvurdering

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.VedtakStatus
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.tilZonedDateTime
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDateTimeNorskTid
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.Beregningsperiode
import no.nav.etterlatte.libs.common.vedtak.BilagMedSammendrag
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

data class Vedtak(
    val id: Long,
    val sakId: Long?,
    val sakType: SakType?,
    val behandlingId: UUID,
    val saksbehandlerId: String?,
    val beregningsResultat: Beregningsresultat?,
    val vilkaarsResultat: JsonNode?,
    val vedtakFattet: Boolean?,
    val fnr: String?,
    val datoFattet: Instant?,
    val datoattestert: Instant?,
    val attestant: String?,
    val virkningsDato: LocalDate?,
    val vedtakStatus: VedtakStatus?,
    val behandlingType: BehandlingType,
    val attestertVedtakEnhet: String?,
    val fattetVedtakEnhet: String?
) {
    fun toDTO(utbetalingsperioder: List<Utbetalingsperiode>) = Vedtak(
        vedtakId = this.id,
        virk = Periode(
            this.virkningsDato?.let(YearMonth::from) ?: YearMonth.now(),
            null
        ), // må få inn dette på toppnivå?
        sak = Sak(this.fnr!!, this.sakType!!, this.sakId!!),
        behandling = Behandling(this.behandlingType, behandlingId),
        type = if (
            this.vilkaarsResultat?.get("resultat")?.get("utfall")?.textValue() == VilkaarsvurderingUtfall.OPPFYLT.name
        ) {
            VedtakType.INNVILGELSE
        } else if (this.behandlingType in listOf(BehandlingType.REVURDERING, BehandlingType.MANUELT_OPPHOER)) {
            VedtakType.OPPHOER
        } else {
            VedtakType.AVSLAG
        }, // Hvor skal vi bestemme vedtakstype?
        grunnlag = emptyList(), // Ikke lenger aktuell
        vilkaarsvurdering = this.vilkaarsResultat, // Bør periodiseres
        beregning = this.beregningsResultat?.let { bres ->
            BilagMedSammendrag(
                objectMapper.valueToTree(bres) as ObjectNode,
                bres.beregningsperioder.map {
                    Beregningsperiode(
                        Periode(
                            YearMonth.from(it.datoFOM),
                            it.datoTOM?.takeIf { it.isBefore(YearMonth.from(LocalDateTime.MAX)) }?.let(YearMonth::from)
                        ),
                        BigDecimal.valueOf(it.utbetaltBeloep.toLong())
                    )
                }
            )
        }, // sammendraget bør lages av beregning
        pensjonTilUtbetaling = utbetalingsperioder,
        vedtakFattet = if (this.vedtakStatus in listOf(
                VedtakStatus.FATTET_VEDTAK,
                VedtakStatus.ATTESTERT,
                VedtakStatus.IVERKSATT
            )
        ) {
            this.saksbehandlerId?.let { ansvarligSaksbehandler ->
                VedtakFattet(
                    ansvarligSaksbehandler,
                    fattetVedtakEnhet!!,
                    this.datoFattet?.tilZonedDateTime()!!
                )
            }
        } else {
            null
        },
        attestasjon = this.attestant?.let { attestant ->
            Attestasjon(
                attestant,
                attestertVedtakEnhet!!,
                this.datoattestert!!.tilZonedDateTime()
            )
        }
    )
}

data class VedtakSammendrag(
    val id: String,
    val behandlingId: UUID,
    val datoAttestert: LocalDateTime?
)

fun no.nav.etterlatte.vedtaksvurdering.Vedtak?.toVedtakSammendrag() = when (this) {
    null -> null
    else -> VedtakSammendrag(
        id = this.id.toString(),
        behandlingId = this.behandlingId,
        datoAttestert = this.datoattestert.toLocalDateTimeNorskTid()
    )
}