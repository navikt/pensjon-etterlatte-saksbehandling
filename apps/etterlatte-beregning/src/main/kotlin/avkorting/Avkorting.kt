package no.nav.etterlatte.avkorting

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.beregning.Beregning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.YearMonth
import java.util.*

data class Avkorting(
    val avkortingGrunnlag: List<AvkortingGrunnlag>,
    val avkortingsperioder: List<Avkortingsperiode>,
    val avkortetYtelse: List<AvkortetYtelse>,
    val aarsoppgjoer: List<Aarsoppgjoer>
) {

    fun kopierAvkorting(): Avkorting = nyAvkorting(
        avkortingGrunnlag = avkortingGrunnlag.map { it.copy(id = UUID.randomUUID()) }
    )

    fun beregnAvkortingNyttEllerEndretGrunnlag(
        nyttGrunnlag: AvkortingGrunnlag,
        virkningstidspunkt: YearMonth,
        beregning: Beregning
    ): Avkorting {
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
        val oppdatertAvkorting = this.copy(avkortingGrunnlag = oppdaterteGrunnlag)
        return oppdatertAvkorting.beregnAvkorting(virkningstidspunkt, beregning)
    }

    fun beregnAvkorting(
        virkningstidspunkt: YearMonth,
        beregning: Beregning,
        forrigeAvkorting: Avkorting? = null
    ): Avkorting {
        val beregnetAarsoppgjoer = AvkortingRegelkjoring.beregnAarsoppgjoer(this, virkningstidspunkt, forrigeAvkorting)
        val beregnetAvkortingsperioder = AvkortingRegelkjoring.beregnInntektsavkorting(
            Periode(fom = virkningstidspunkt, tom = null),
            avkortingGrunnlag
        )
        val beregnetAvkortetYtelse = AvkortingRegelkjoring.beregnAvkortetYtelse(
            Periode(fom = virkningstidspunkt, null),
            beregning.beregningsperioder,
            beregnetAvkortingsperioder,
            beregnetAarsoppgjoer.last().restanse
        )

        return this.copy(
            aarsoppgjoer = beregnetAarsoppgjoer.let {
                val oppgjoerTidligereAar = aarsoppgjoer.filter { it.maaned.year != beregnetAarsoppgjoer[0].maaned.year }
                oppgjoerTidligereAar + beregnetAarsoppgjoer
            },
            avkortingsperioder = beregnetAvkortingsperioder,
            avkortetYtelse = beregnetAvkortetYtelse
        )
    }

    fun hentAktiveInntektsgrunnlag(): AvkortingGrunnlag =
        avkortingGrunnlag.find { it.periode.tom == null } ?: throw Exception("Fant ingen løpende grunnlag")

    companion object {
        fun nyAvkorting(
            avkortingGrunnlag: List<AvkortingGrunnlag> = emptyList()
        ) = Avkorting(
            avkortingGrunnlag = avkortingGrunnlag,
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

fun List<Avkortingsperiode>.avkortingIPeriode(maaned: YearMonth) = this.find {
    maaned >= it.periode.fom && (it.periode.tom == null || maaned <= it.periode.tom)
}?.avkorting ?: throw Exception("Perioder ny inntektsavkorting stemmer ikke overens med tidligere avkortinger")

data class AvkortetYtelse(
    val periode: Periode,
    val ytelseEtterAvkorting: Int,
    val restanse: Int = 0,
    val avkortingsbeloep: Int,
    val ytelseFoerAvkorting: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode,
    val kilde: Grunnlagsopplysning.RegelKilde
)

data class Aarsoppgjoer(
    val maaned: YearMonth,
    val avkortingForventetInntekt: Int,
    val tidligereAvkorting: Int,
    val restanse: Int,
    val nyAvkorting: Int
)