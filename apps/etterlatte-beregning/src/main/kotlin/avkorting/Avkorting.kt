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
            avkortingsperioder
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

        // TODO EY-2368 doc - dokumenter hvorfor inntektsavkorting må beregnes på nytt tilake i tid
        val fraFoersteMaaned = Periode(fom = aarsoppgjoer.first().maaned, tom = null)
        val avkortingHeleAaret = AvkortingRegelkjoring.beregnInntektsavkorting(
            periode = fraFoersteMaaned,
            avkortingGrunnlag = listOf(avkortingGrunnlag.last().copy(periode = fraFoersteMaaned))
        )

        val aarsoppgjoerNyBeregningOgAvkorting = aarsoppgjoer.map {
            it.copy(avkorting = avkortingHeleAaret.avkortingIMaaned(it.maaned))
        }.oppdaterEtterVirk(virkningstidspunkt) {
            it.copy(beregning = beregning.beregningsperioder.beregningIMaaned(it.maaned))
        }

        val avkortetYtelseFoerVirk = AvkortingRegelkjoring.beregnAvkortetYtelsePaaNytt(
            aarsoppgjoerNyBeregningOgAvkorting.filter { it.maaned < virkningstidspunkt }
        )
        val aarsoppgjoerMedNyAvkortetYtelse = aarsoppgjoerNyBeregningOgAvkorting.oppdaterFoerVirk(virkningstidspunkt) {
            it.copy(
                forventetAvkortetYtelse = avkortetYtelseFoerVirk.avkortetYtelseIMaaned(it.maaned).ytelseEtterAvkorting,
            )
        }

        val restanseFoerVirk = AvkortingRegelkjoring.beregnRestanse(
            aarsoppgjoerMedNyAvkortetYtelse.filter { it.maaned < virkningstidspunkt }
        )
        val aarsoppgjoerMedRestanseFoerVirk = aarsoppgjoerMedNyAvkortetYtelse.oppdaterFoerVirk(virkningstidspunkt) {
            it.copy(
                restanse = restanseFoerVirk[it.maaned]?.verdi ?: throw Exception("") // TODO EY-2368
            )
        }

        val fordeltRestanse =
            AvkortingRegelkjoring.beregnFordeltRestanse(virkningstidspunkt, aarsoppgjoerMedRestanseFoerVirk)
        val aarsoppgjoerMedFordeltRestanse = aarsoppgjoerMedRestanseFoerVirk.oppdaterEtterVirk(virkningstidspunkt) {
            it.copy(
                fordeltRestanse = fordeltRestanse.verdi // TODO EY-2368
            )
        }

        val avkortetYtelseFraNyVirk = AvkortingRegelkjoring.beregnAvkortetYtelse(
            Periode(fom = virkningstidspunkt, null),
            beregning.beregningsperioder,
            avkortingHeleAaret,
            fordeltRestanse
        )
        val aarsoppgjoerMedYtelseEtterAvkorting = aarsoppgjoerMedFordeltRestanse.oppdaterEtterVirk(virkningstidspunkt) {
            val avkortetYtelse = avkortetYtelseFraNyVirk.avkortetYtelseIMaaned(it.maaned)
            it.copy(
                forventetAvkortetYtelse = avkortetYtelse.ytelseEtterAvkortingFoerRestanse,
                faktiskAvkortetYtelse = avkortetYtelse.ytelseEtterAvkorting
            )
        }

        return this.copy(
            avkortingsperioder = avkortingHeleAaret,
            aarsoppgjoer = aarsoppgjoerMedYtelseEtterAvkorting,
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

// TODO EY-2368 rename til restanseopppgjoer.. for å tydeliggjøre hva det faktisk brukes til
data class AarsoppgjoerMaaned(
    val maaned: YearMonth,
    val beregning: Int,
    val avkorting: Int,
    val forventetAvkortetYtelse: Int,
    val restanse: Int, // TODO EY-2368 Restanse, ikke optional men default verdi 0 og regel og kilde optinal istedet
    val fordeltRestanse: Int, // TODO FordeltRestanse
    val faktiskAvkortetYtelse: Int
)

fun List<AarsoppgjoerMaaned>.oppdaterFoerVirk(
    virkningstidspunkt: YearMonth,
    oppdaterFoerVirk: (maaned: AarsoppgjoerMaaned) -> AarsoppgjoerMaaned
) = map {
    if (it.maaned < virkningstidspunkt) {
        oppdaterFoerVirk(it)
    } else {
        it
    }
}

fun List<AarsoppgjoerMaaned>.oppdaterEtterVirk(
    virkningstidspunkt: YearMonth,
    oppdaterFoerVirk: (maaned: AarsoppgjoerMaaned) -> AarsoppgjoerMaaned
) = map {
    if (it.maaned >= virkningstidspunkt) {
        oppdaterFoerVirk(it)
    } else {
        it
    }
}

data class Restanse(
    val verdi: Int,
    val regelResultat: JsonNode,
    val kilde: Grunnlagsopplysning.RegelKilde
)

data class FordeltRestanse(
    val verdi: Int,
    val regelResultat: JsonNode,
    val kilde: Grunnlagsopplysning.RegelKilde
)

data class AvkortetYtelse(
    val periode: Periode,
    val ytelseEtterAvkorting: Int,
    val ytelseEtterAvkortingFoerRestanse: Int,
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