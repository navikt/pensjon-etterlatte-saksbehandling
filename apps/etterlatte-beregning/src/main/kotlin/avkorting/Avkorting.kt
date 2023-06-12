package no.nav.etterlatte.avkorting

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.*

data class Avkorting(
    val avkortingGrunnlag: List<AvkortingGrunnlag>,
    val avkortingsperioder: List<Avkortingsperiode>,
    val avkortetYtelse: List<AvkortetYtelse>
) {
    fun leggTilEllerOppdaterGrunnlag(nyttGrunnlag: AvkortingGrunnlag): Avkorting {
        val oppdaterteGrunnlag = avkortingGrunnlag
            .filter { it.id != nyttGrunnlag.id }
            .map {
                when (it.periode.tom) {
                    null -> it.copy(
                        periode = Periode(fom = it.periode.fom, tom = nyttGrunnlag.periode.fom.minusMonths(1))
                    )

                    else -> it
                }
            } + listOf(nyttGrunnlag)
        return this.copy(avkortingGrunnlag = oppdaterteGrunnlag)
    }

    fun oppdaterAvkortingMedNyeBeregninger(
        nyeAvkortingsperioder: List<Avkortingsperiode>,
        nyAvkortetYtelse: List<AvkortetYtelse>
    ): Avkorting = this.copy(
        avkortingsperioder = nyeAvkortingsperioder,
        avkortetYtelse = nyAvkortetYtelse
    )

    companion object {
        fun nyAvkorting() = Avkorting(
            emptyList(),
            emptyList(),
            emptyList()
        )
    }
}

data class AvkortingGrunnlag(
    val id: UUID,
    val periode: Periode,
    val aarsinntekt: Int,
    val fratrekkInnAar: Int,
    val relevanteMaanederInnAar: Int,
    val spesifikasjon: String,
    val kilde: Grunnlagsopplysning.Saksbehandler
)

data class Avkortingsperiode(
    val periode: Periode,
    val avkorting: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode,
    val kilde: Grunnlagsopplysning.RegelKilde
)

data class AvkortetYtelse(
    val periode: Periode,
    val ytelseEtterAvkorting: Int,
    val avkortingsbeloep: Int,
    val ytelseFoerAvkorting: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode,
    val kilde: Grunnlagsopplysning.RegelKilde
)