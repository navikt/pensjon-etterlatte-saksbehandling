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
) {

    fun kopierAvkorting(virkningstidspunkt: YearMonth): Avkorting = Avkorting(
        aarsoppgjoer = aarsoppgjoer.copy(
            ytelseFoerAvkorting = aarsoppgjoer.ytelseFoerAvkorting.map { it },
            inntektsavkorting = aarsoppgjoer.inntektsavkorting.map {
                it.copy(
                    grunnlag = it.grunnlag.copy(
                        id = UUID.randomUUID(),
                        virkningstidspunkt = virkningstidspunkt
                    )
                )
            },
            avkortetYtelseForrigeVedtak = aarsoppgjoer.avkortetYtelseAar.map { it.copy(id = UUID.randomUUID()) }
        )
    )

    fun beregnAvkortingMedNyttGrunnlag(
        nyttGrunnlag: AvkortingGrunnlag,
        behandlingstype: BehandlingType,
        beregning: Beregning
    ) = oppdaterMedInntektsgrunnlag(nyttGrunnlag).beregnAvkorting(behandlingstype, beregning)

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

    fun beregnAvkorting(behandlingstype: BehandlingType, beregning: Beregning): Avkorting =
        if (behandlingstype == BehandlingType.FØRSTEGANGSBEHANDLING) {
            beregnAvkortingForstegangs(beregning)
        } else {
            beregnAvkortingRevurdering(beregning)
        }

    fun finnLoependeYtelse(): List<AvkortetYtelse> {
        val virkningstidspunkt = virkningstidspunkt()
        return aarsoppgjoer.avkortetYtelseAar.filter { it.periode.fom >= virkningstidspunkt }.map {
            if (virkningstidspunkt > it.periode.fom && (it.periode.tom == null || virkningstidspunkt <= it.periode.tom)) {
                it.copy(periode = Periode(fom = virkningstidspunkt, tom = it.periode.tom))
            } else {
                it
            }
        }
    }

    private fun beregnAvkortingForstegangs(beregning: Beregning): Avkorting {

        val ytelseFoerAvkorting = beregning.mapTilYtelseFoerAvkorting()


        val grunnlag = aarsoppgjoer.inntektsavkorting.first().grunnlag

        val avkortingsperioder = AvkortingRegelkjoring.beregnInntektsavkorting(
            Periode(fom = grunnlag.virkningstidspunkt, tom = null),
            listOf(grunnlag)
        )

        val beregnetAvkortetYtelse = AvkortingRegelkjoring.beregnAvkortetYtelse(
            periode = Periode(fom = grunnlag.virkningstidspunkt, tom = null),
            ytelseFoerAvkorting = ytelseFoerAvkorting,
            avkortingsperioder = avkortingsperioder,
            type = AvkortetYtelseType.INNTEKT
        )

        return this.copy(
            aarsoppgjoer = this.aarsoppgjoer.copy(
                ytelseFoerAvkorting = ytelseFoerAvkorting,
                inntektsavkorting = this.aarsoppgjoer.inntektsavkorting.map {
                    it.copy(
                        avkortetYtelse = beregnetAvkortetYtelse.map { it.copy(inntektsgrunnlag = grunnlag.id) }
                    )
                },
                avkortetYtelseAar = beregnetAvkortetYtelse
            ),
        )
    }

    private fun beregnAvkortingRevurdering(beregning: Beregning): Avkorting {

        val ytelseFoerAvkorting =
            this.aarsoppgjoer.ytelseFoerAvkorting.leggTilNyeBeregninger(beregning, virkningstidspunkt())

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
                inntektsavkorting.grunnlag.periode.fom,
                avkortetYtelseMedAllForventetInntekt,
                inntektsavkorting.avkortetYtelse
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

    private fun virkningstidspunkt() = this.aarsoppgjoer.inntektsavkorting.first().grunnlag.virkningstidspunkt
}

data class AvkortingGrunnlag(
    val id: UUID,
    val periode: Periode,
    val aarsinntekt: Int,
    val fratrekkInnAar: Int,
    val relevanteMaanederInnAar: Int,
    val spesifikasjon: String,
    val kilde: Grunnlagsopplysning.Saksbehandler,
    val virkningstidspunkt: YearMonth
)

data class Aarsoppgjoer(
    val ytelseFoerAvkorting: List<YtelseFoerAvkorting> = emptyList(),
    val inntektsavkorting: List<Inntektsavkorting> = emptyList(),
    val avkortetYtelseAar: List<AvkortetYtelse> = emptyList(),
    val avkortetYtelseForrigeVedtak: List<AvkortetYtelse> = emptyList()
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
    // TODO fra og med dato? antall gjenværende?
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
    val restanse: Int = 0, // TODO klasse med regelresultat
    val avkortingsbeloep: Int,
    val ytelseFoerAvkorting: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode,
    val kilde: Grunnlagsopplysning.RegelKilde,
    val inntektsgrunnlag: UUID? = null
)

enum class AvkortetYtelseType { INNTEKT, AARSOPPGJOER, FORRIGE_VEDTAK, ETTEROPPJOER }

fun Beregning.mapTilYtelseFoerAvkorting() = beregningsperioder.map {
    YtelseFoerAvkorting(
        beregning = it.utbetaltBeloep,
        periode = Periode(it.datoFOM, it.datoTOM),
        beregningsreferanse = this.beregningId
    )
}

// TODO EY-2523 unittest
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
