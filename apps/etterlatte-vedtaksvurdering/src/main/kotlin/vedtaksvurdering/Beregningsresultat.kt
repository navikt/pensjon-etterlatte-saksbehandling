package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import java.time.LocalDateTime
import java.util.*

data class Beregningsresultat(
    val id: UUID,
    val type: Beregningstype,
    val beregningsperioder: List<Beregningsperiode>,
    val beregnetDato: LocalDateTime,
    val grunnlagVersjon: Long
) {
    companion object {
        fun fraDto(beregning: BeregningDTO) = Beregningsresultat(
            id = beregning.beregningId,
            type = beregning.type,
            beregningsperioder = beregning.beregningsperioder,
            beregnetDato = LocalDateTime.from(beregning.beregnetDato.toNorskTid()),
            grunnlagVersjon = beregning.grunnlagMetadata.versjon
        )
    }
}