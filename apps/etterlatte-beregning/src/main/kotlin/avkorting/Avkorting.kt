package no.nav.etterlatte.avkorting

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.beregning.Beregning
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.YearMonth
import java.util.*

data class Avkorting(
    val avkortingGrunnlag: List<AvkortingGrunnlag>,
    val avkortingsperioder: List<Avkortingsperiode>,
    val aarsoppgjoer: List<AarsoppgjoerMaaned>,
    val avkortetYtelse: List<AvkortetYtelse>
) {

    fun kopierAvkorting(): Avkorting = nyAvkorting(
        avkortingGrunnlag = avkortingGrunnlag.map { it.copy(id = UUID.randomUUID()) },
        restanseOppgjoer = this.aarsoppgjoer
    )

    fun beregnAvkortingMedNyttGrunnlag(
        nyttGrunnlag: AvkortingGrunnlag,
        behandlingstype: BehandlingType,
        virkningstidspunkt: YearMonth,
        beregning: Beregning
    ) = oppdaterMedInntektsgrunnlag(nyttGrunnlag).beregnAvkorting(behandlingstype, virkningstidspunkt, beregning)

    fun oppdaterMedInntektsgrunnlag(
        nyttGrunnlag: AvkortingGrunnlag
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

        return this.copy(avkortingGrunnlag = oppdaterteGrunnlag)
    }

    fun beregnAvkorting(
        behandlingstype: BehandlingType,
        virkningstidspunkt: YearMonth,
        beregning: Beregning
    ): Avkorting = if (behandlingstype == BehandlingType.FØRSTEGANGSBEHANDLING) {
        beregnAvkortingForstegangs(virkningstidspunkt, beregning)
    } else {
        beregnAvkortingRevurdering(virkningstidspunkt, beregning)
    }

    private fun beregnAvkortingForstegangs(
        virkningstidspunkt: YearMonth,
        beregning: Beregning
    ): Avkorting {
        val avkortingsperioder = AvkortingRegelkjoring.beregnInntektsavkorting(
            periode = Periode(fom = virkningstidspunkt, tom = null),
            avkortingGrunnlag
        )

        val beregnetAvkortetYtelse = AvkortingRegelkjoring.beregnAvkortetYtelse(
            Periode(fom = virkningstidspunkt, null),
            beregning.beregningsperioder,
            avkortingsperioder,
            0
        )

        val aarsoppgjoer = mutableListOf<AarsoppgjoerMaaned>()
        for (maanednr in virkningstidspunkt.monthValue..12) {
            val maaned = YearMonth.of(virkningstidspunkt.year, maanednr)
            aarsoppgjoer.add(
                AarsoppgjoerMaaned(
                    maaned = maaned,
                    beregning = beregning.beregningsperioder.beregningIMaaned(maaned),
                    avkorting = avkortingsperioder.avkortingIMaaned(maaned),
                    forventetAvkortetYtelse = beregnetAvkortetYtelse.avkortetYtelseIMaaned(maaned).ytelseEtterAvkorting,
                    restanse = 0,
                    fordeltRestanse = 0,
                    faktiskAvkortetYtelse = beregnetAvkortetYtelse.avkortetYtelseIMaaned(maaned).ytelseEtterAvkorting
                )
            )
        }

        return this.copy(
            avkortingsperioder = avkortingsperioder,
            aarsoppgjoer = aarsoppgjoer,
            avkortetYtelse = beregnetAvkortetYtelse
        )
    }

    private fun beregnAvkortingRevurdering(
        virkningstidspunkt: YearMonth,
        beregning: Beregning
    ): Avkorting {
        if (aarsoppgjoer.isEmpty()) {
            throw IllegalArgumentException("Det må være et oppprettet årsoppgjør for å kunne beregne")
        }

        val avkortingsperioder = with(aarsoppgjoer.first()) {
            AvkortingRegelkjoring.beregnInntektsavkorting(
                periode = Periode(fom = maaned, tom = null),
                listOf(avkortingGrunnlag.last().copy(periode = Periode(fom = maaned, tom = null)))
            )
        }

        val aarsoppgjoerMedNyAvkorting = aarsoppgjoer.map {
            it.copy(avkorting = avkortingsperioder.avkortingIMaaned(it.maaned))
        }

        val restanseFoerVirk = aarsoppgjoerMedNyAvkorting.filter { it.maaned < virkningstidspunkt }.map {
            AvkortingRegelkjoring.beregnRestanse(it)
        }
        val aarsoppgjoerMedUtregnetRestanse = aarsoppgjoerMedNyAvkorting.map { aarsoppgjoerMaaned ->
            if (aarsoppgjoerMaaned.maaned < virkningstidspunkt) {
                val beregnetRestanse = restanseFoerVirk.oppgjoerIMaaned(aarsoppgjoerMaaned.maaned)
                aarsoppgjoerMaaned.copy(
                    forventetAvkortetYtelse = beregnetRestanse.forventetAvkortetYtelse,
                    restanse = beregnetRestanse.restanse
                )
            } else {
                aarsoppgjoerMaaned
            }
        }

        val maanedligRestanse =
            AvkortingRegelkjoring.beregnFordeltRestanse(virkningstidspunkt, aarsoppgjoerMedUtregnetRestanse)

        val avkortetYtelseFraNyVirk = AvkortingRegelkjoring.beregnAvkortetYtelse(
            Periode(fom = virkningstidspunkt, null),
            beregning.beregningsperioder,
            avkortingsperioder,
            maanedligRestanse
        )

        val aarsoppgjoerMedFordeltRestanse = aarsoppgjoerMedUtregnetRestanse.map {
            if (it.maaned >= virkningstidspunkt) {
                val avkortetYtelse = avkortetYtelseFraNyVirk.avkortetYtelseIMaaned(it.maaned)
                it.copy(
                    forventetAvkortetYtelse = avkortetYtelse.ytelseEtterAvkortingFoerRestanse,
                    fordeltRestanse = avkortetYtelse.restanse,
                    faktiskAvkortetYtelse = avkortetYtelse.ytelseEtterAvkorting
                )
            } else {
                it
            }
        }

        return this.copy(
            avkortingsperioder = avkortingsperioder,
            aarsoppgjoer = aarsoppgjoerMedFordeltRestanse,
            avkortetYtelse = avkortetYtelseFraNyVirk
        )
    }

    companion object {
        fun nyAvkorting(
            avkortingGrunnlag: List<AvkortingGrunnlag> = emptyList(),
            restanseOppgjoer: List<AarsoppgjoerMaaned> = emptyList()
        ) = Avkorting(
            avkortingGrunnlag = avkortingGrunnlag,
            avkortingsperioder = emptyList(),
            aarsoppgjoer = restanseOppgjoer,
            avkortetYtelse = emptyList()
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

data class AarsoppgjoerMaaned(
    val maaned: YearMonth,
    val beregning: Int,
    val avkorting: Int,
    val forventetAvkortetYtelse: Int,
    val restanse: Int, // TODO EY-2368 persistere regelresultat
    val fordeltRestanse: Int,
    val faktiskAvkortetYtelse: Int
)

data class AvkortetYtelse(
    val periode: Periode,
    val ytelseEtterAvkorting: Int,
    val ytelseEtterAvkortingFoerRestanse: Int, // TODO EY-2368 Legge til i DB
    val restanse: Int = 0,
    val avkortingsbeloep: Int,
    val ytelseFoerAvkorting: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode,
    val kilde: Grunnlagsopplysning.RegelKilde
)

fun List<AvkortetYtelse>.avkortetYtelseIMaaned(maaned: YearMonth) = this.find {
    maaned >= it.periode.fom && (it.periode.tom == null || maaned <= it.periode.tom)
} ?: throw Exception("Maaned finnes ikke i avkortet ytelse sin periode")

fun List<Avkortingsperiode>.avkortingIMaaned(maaned: YearMonth) = this.find {
    maaned >= it.periode.fom && (it.periode.tom == null || maaned <= it.periode.tom)
}?.avkorting ?: throw Exception("Maaned finnes ikke i avkortingsperioder")

fun List<Beregningsperiode>.beregningIMaaned(maaned: YearMonth) = this.find {
    maaned >= it.datoFOM && (it.datoTOM == null || maaned <= it.datoTOM)
}?.utbetaltBeloep ?: throw Exception("Maaned finnes ikke i beregningsperioder")

fun List<AarsoppgjoerMaaned>.oppgjoerIMaaned(maaned: YearMonth) = this.find {
    it.maaned == maaned
} ?: throw Exception("Maaned finnes ikke i aarsoppgjoeret")