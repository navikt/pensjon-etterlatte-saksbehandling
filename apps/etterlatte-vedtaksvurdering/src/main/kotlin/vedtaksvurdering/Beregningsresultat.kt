package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.BeregningsResultatType
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.beregning.Endringskode
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import java.time.LocalDateTime
import java.util.*

data class Beregningsresultat(
    val id: UUID,
    val type: Beregningstyper,
    val endringskode: Endringskode,
    val resultat: BeregningsResultatType,
    val beregningsperioder: List<Beregningsperiode>,
    val beregnetDato: LocalDateTime,
    val grunnlagVersjon: Long = 0L
) {
    companion object {
        fun fraDto(beregning: BeregningDTO) = Beregningsresultat(
            id = beregning.beregningId,
            type = Beregningstyper.GP,
            endringskode = Endringskode.NY,
            resultat = BeregningsResultatType.BEREGNET,
            beregningsperioder = beregning.beregningsperioder,
            beregnetDato = LocalDateTime.from(beregning.beregnetDato.toNorskTid()),
            grunnlagVersjon = beregning.grunnlagMetadata.versjon
        )
    }
}