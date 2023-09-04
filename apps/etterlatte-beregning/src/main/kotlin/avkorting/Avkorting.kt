package no.nav.etterlatte.avkorting

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.beregning.Beregning
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.YearMonth
import java.util.*

data class Avkorting(
    val aarsoppgjoer: Aarsoppgjoer = Aarsoppgjoer(),
    val lopendeYtelse: List<AvkortetYtelse> = emptyList(), // Fylles ut av metode medLoependeYtelse
    val avkortetYtelseForrigeVedtak: List<AvkortetYtelse> = emptyList()
) {

    /*
    * Årsoppgjør inneholder ytelsen sine perioder for hele år og for behandlinger/vedtak er det kun
    * virknigstidspunkt og fremover som er relevant.
    */
    fun medLoependeYtelse(
        virkningstidspunkt: YearMonth,
        forrigeAvkorting: Avkorting? = null
    ): Avkorting = this.copy(
        lopendeYtelse = aarsoppgjoer.avkortetYtelseAar.filter { it.periode.fom >= virkningstidspunkt }.map {
            if (virkningstidspunkt > it.periode.fom && (it.periode.tom == null || virkningstidspunkt <= it.periode.tom)) {
                it.copy(periode = Periode(fom = virkningstidspunkt, tom = it.periode.tom))
            } else {
                it
            }
        },
        avkortetYtelseForrigeVedtak = forrigeAvkorting?.aarsoppgjoer?.avkortetYtelseAar ?: emptyList()
    )

    fun kopierAvkorting(): Avkorting = Avkorting(
        aarsoppgjoer = aarsoppgjoer.copy(
            ytelseFoerAvkorting = aarsoppgjoer.ytelseFoerAvkorting.map { it },
            inntektsavkorting = aarsoppgjoer.inntektsavkorting.map { inntektsavkorting ->
                inntektsavkorting.copy(grunnlag = inntektsavkorting.grunnlag.copy(id = UUID.randomUUID()))
            }
        )
    )

    fun beregnAvkortingMedNyttGrunnlag(
        nyttGrunnlag: AvkortingGrunnlag,
        virkningstidspunkt: YearMonth,
        behandlingstype: BehandlingType,
        beregning: Beregning
    ) = oppdaterMedInntektsgrunnlag(nyttGrunnlag).beregnAvkorting(virkningstidspunkt, behandlingstype, beregning)

    fun oppdaterMedInntektsgrunnlag(
        nyttGrunnlag: AvkortingGrunnlag
    ): Avkorting {
        val inntektsavkorting = aarsoppgjoer.inntektsavkorting
            .filter { it.grunnlag.id != nyttGrunnlag.id }
            .map {
                when (it.grunnlag.periode.tom) {
                    null -> it.copy(
                        grunnlag = it.grunnlag.copy(
                            periode = Periode(
                                fom = it.grunnlag.periode.fom,
                                tom = nyttGrunnlag.periode.fom.minusMonths(1)
                            )
                        ),
                    )

                    else -> it
                }
            } + listOf(Inntektsavkorting(grunnlag = nyttGrunnlag))
        return this.copy(
            aarsoppgjoer = aarsoppgjoer.copy(
                inntektsavkorting = inntektsavkorting
            )
        )
    }

    fun beregnAvkorting(
        virkningstidspunkt: YearMonth,
        behandlingstype: BehandlingType,
        beregning: Beregning
    ): Avkorting =
        if (behandlingstype == BehandlingType.FØRSTEGANGSBEHANDLING) {
            beregnAvkortingForstegangs(virkningstidspunkt, beregning)
        } else {
            beregnAvkortingRevurdering(virkningstidspunkt, beregning)
        }

    private fun beregnAvkortingForstegangs(virkningstidspunkt: YearMonth, beregning: Beregning): Avkorting {

        val ytelseFoerAvkorting = beregning.mapTilYtelseFoerAvkorting()
        val grunnlag = aarsoppgjoer.inntektsavkorting.first().grunnlag

        val avkortingsperioder = AvkortingRegelkjoring.beregnInntektsavkorting(
            Periode(fom = virkningstidspunkt, tom = null),
            listOf(grunnlag)
        )

        val beregnetAvkortetYtelse = AvkortingRegelkjoring.beregnAvkortetYtelse(
            periode = Periode(fom = virkningstidspunkt, tom = null),
            ytelseFoerAvkorting = ytelseFoerAvkorting,
            avkortingsperioder = avkortingsperioder,
            type = AvkortetYtelseType.INNTEKT
        )

        return this.copy(
            aarsoppgjoer = this.aarsoppgjoer.copy(
                ytelseFoerAvkorting = ytelseFoerAvkorting,
                inntektsavkorting = this.aarsoppgjoer.inntektsavkorting.map {
                    it.copy(
                        avkortingsperioder = avkortingsperioder,
                        avkortetYtelse = beregnetAvkortetYtelse.map { it.copy(inntektsgrunnlag = grunnlag.id) }
                    )
                },
                avkortetYtelseAar = beregnetAvkortetYtelse.map {
                    it.copy(
                        id = UUID.randomUUID(),
                        type = AvkortetYtelseType.AARSOPPGJOER
                    )
                }
            ),
        )
    }

    private fun beregnAvkortingRevurdering(virkningstidspunkt: YearMonth, beregning: Beregning): Avkorting {

        val ytelseFoerAvkorting =
            this.aarsoppgjoer.ytelseFoerAvkorting.leggTilNyeBeregninger(beregning, virkningstidspunkt)

        val reberegnetInntektsavkorting = this.aarsoppgjoer.inntektsavkorting.map { inntektsavkorting ->
            val periode = Periode(fom = this.foersteMaanedDetteAar(), tom = inntektsavkorting.grunnlag.periode.tom)

            val avkortinger = AvkortingRegelkjoring.beregnInntektsavkorting(
                periode = periode,
                avkortingGrunnlag = listOf(inntektsavkorting.grunnlag.copy(periode = periode))
            )

            val avkortetYtelseMedInntekt = AvkortingRegelkjoring.beregnAvkortetYtelse(
                periode = periode,
                ytelseFoerAvkorting = ytelseFoerAvkorting,
                avkortingsperioder = avkortinger,
                type = AvkortetYtelseType.INNTEKT
            )

            inntektsavkorting.copy(
                avkortingsperioder = avkortinger,
                avkortetYtelse = avkortetYtelseMedInntekt.map { it.copy(inntektsgrunnlag = inntektsavkorting.grunnlag.id) }
            )
        }

        val avkortetYtelseMedAllForventetInntekt = mutableListOf<AvkortetYtelse>()
        reberegnetInntektsavkorting.forEach { inntektsavkorting ->
            val restanse = AvkortingRegelkjoring.beregnRestanse(
                this.foersteMaanedDetteAar(),
                inntektsavkorting,
                avkortetYtelseMedAllForventetInntekt
            )
            val ytelse = AvkortingRegelkjoring.beregnAvkortetYtelse(
                periode = inntektsavkorting.grunnlag.periode,
                ytelseFoerAvkorting = ytelseFoerAvkorting,
                avkortingsperioder = inntektsavkorting.avkortingsperioder,
                type = AvkortetYtelseType.AARSOPPGJOER,
                restanse,
            )
            avkortetYtelseMedAllForventetInntekt.addAll(ytelse)
        }

        return this.copy(
            aarsoppgjoer = this.aarsoppgjoer.copy(
                ytelseFoerAvkorting = ytelseFoerAvkorting,
                inntektsavkorting = reberegnetInntektsavkorting,
                avkortetYtelseAar = avkortetYtelseMedAllForventetInntekt
            ),
        )
    }

    private fun foersteMaanedDetteAar() = this.aarsoppgjoer.ytelseFoerAvkorting.first().periode.fom

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

data class Aarsoppgjoer(
    val ytelseFoerAvkorting: List<YtelseFoerAvkorting> = emptyList(),
    val inntektsavkorting: List<Inntektsavkorting> = emptyList(),
    val avkortetYtelseAar: List<AvkortetYtelse> = emptyList()
)

data class Inntektsavkorting(
    val grunnlag: AvkortingGrunnlag,
    val avkortingsperioder: List<Avkortingsperiode> = emptyList(),
    val avkortetYtelse: List<AvkortetYtelse> = emptyList()
)

data class YtelseFoerAvkorting(
    val beregning: Int,
    val periode: Periode,
    val beregningsreferanse: UUID
)

data class Avkortingsperiode(
    val id: UUID,
    val periode: Periode,
    val avkorting: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode,
    val kilde: Grunnlagsopplysning.RegelKilde,
    val inntektsgrunnlag: UUID
)

data class Restanse(
    val id: UUID,
    val totalRestanse: Int,
    val fordeltRestanse: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode?,
    val kilde: Grunnlagsopplysning.Kilde,
)

data class AvkortetYtelse(
    val id: UUID,
    val type: AvkortetYtelseType,
    val periode: Periode,
    val ytelseEtterAvkorting: Int,
    val ytelseEtterAvkortingFoerRestanse: Int,
    val restanse: Restanse?,
    val avkortingsbeloep: Int,
    val ytelseFoerAvkorting: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode,
    val kilde: Grunnlagsopplysning.RegelKilde,
    val inntektsgrunnlag: UUID? = null
)

enum class AvkortetYtelseType { INNTEKT, AARSOPPGJOER, ETTEROPPJOER }

fun Beregning.mapTilYtelseFoerAvkorting() = beregningsperioder.map {
    YtelseFoerAvkorting(
        beregning = it.utbetaltBeloep,
        periode = Periode(it.datoFOM, it.datoTOM),
        beregningsreferanse = this.beregningId
    )
}

fun List<YtelseFoerAvkorting>.leggTilNyeBeregninger(
    beregning: Beregning,
    virkningstidspunkt: YearMonth
) = filter { it.periode.fom < virkningstidspunkt }
    .filter { beregning.beregningId != it.beregningsreferanse }.map { ytelseFoerAvkorting ->
        if (ytelseFoerAvkorting.periode.tom != null) {
            if (virkningstidspunkt == ytelseFoerAvkorting.periode.tom) {
                ytelseFoerAvkorting.copy(
                    periode = Periode(
                        fom = ytelseFoerAvkorting.periode.fom,
                        tom = virkningstidspunkt.minusMonths(1)
                    )
                )
            } else if (virkningstidspunkt < ytelseFoerAvkorting.periode.tom) {
                ytelseFoerAvkorting.copy(periode = Periode(fom = ytelseFoerAvkorting.periode.fom, tom = null))
            } else {
                ytelseFoerAvkorting
            }
        } else {
            ytelseFoerAvkorting.copy(
                periode = Periode(
                    fom = ytelseFoerAvkorting.periode.fom,
                    tom = virkningstidspunkt.minusMonths(1)
                )
            )
        }
    } + beregning.mapTilYtelseFoerAvkorting()
