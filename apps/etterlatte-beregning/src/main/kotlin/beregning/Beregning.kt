package no.nav.etterlatte.beregning

import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

data class Beregning(
    val beregningId: UUID,
    val behandlingId: UUID,
    val type: Beregningstype,
    val beregningsperioder: List<Beregningsperiode>,
    val beregnetDato: Tidspunkt,
    val grunnlagMetadata: Metadata,
) {
    fun toDTO() =
        BeregningDTO(
            beregningId = beregningId,
            behandlingId = behandlingId,
            type = type,
            beregningsperioder = beregningsperioder,
            beregnetDato = beregnetDato,
            grunnlagMetadata = grunnlagMetadata,
        )
}
