@file:Suppress("ktlint:standard:no-wildcard-imports")

package no.nav.etterlatte.beregning

import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.beregning.OverstyrBeregningDTO
import no.nav.etterlatte.libs.common.beregning.OverstyrtBeregningKategori
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.sak.SakId
import java.util.UUID

data class Beregning(
    val beregningId: UUID,
    val behandlingId: UUID,
    val type: Beregningstype,
    val beregningsperioder: List<Beregningsperiode>,
    val beregnetDato: Tidspunkt,
    val grunnlagMetadata: Metadata,
    val overstyrBeregning: OverstyrBeregning?,
) {
    fun toDTO() =
        BeregningDTO(
            beregningId = beregningId,
            behandlingId = behandlingId,
            type = type,
            beregningsperioder = beregningsperioder,
            beregnetDato = beregnetDato,
            grunnlagMetadata = grunnlagMetadata,
            overstyrBeregning = overstyrBeregning?.toDTO(),
        )
}

data class OverstyrBeregning(
    val sakId: SakId,
    val beskrivelse: String,
    val tidspunkt: Tidspunkt,
    val status: OverstyrBeregningStatus = OverstyrBeregningStatus.AKTIV,
    val kategori: OverstyrtBeregningKategori,
)

fun OverstyrBeregning.toDTO() = OverstyrBeregningDTO(this.beskrivelse, this.kategori)
