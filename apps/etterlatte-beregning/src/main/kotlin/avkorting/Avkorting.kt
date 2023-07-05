package no.nav.etterlatte.avkorting

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.beregning.Beregning
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.YearMonth
import java.util.*

data class Avkorting(
    val avkortingGrunnlag: List<AvkortingGrunnlag>,
    val aarsoppgjoer: Aarsoppgjoer,
    val avkortetYtelse: List<AvkortetYtelse>
) {

    fun kopierAvkorting(virkningstidspunkt: YearMonth): Avkorting = nyAvkorting(
        avkortingGrunnlag = avkortingGrunnlag.map { it.copy(id = UUID.randomUUID()) },
        tidligereAvkortetYtelse = aarsoppgjoer.tidligereAvkortetYtelse + avkortetYtelse.map {
            when (it.periode.tom) {
                null -> it.copy(
                    type = AvkortetYtelseType.TIDLIGERE,
                    periode = Periode(fom = it.periode.fom, tom = virkningstidspunkt.minusMonths(1))
                )

                else -> it.copy(
                    type = AvkortetYtelseType.TIDLIGERE
                )
            }
        },
        ytelseFoerAvkorting = aarsoppgjoer.ytelseFoerAvkorting,
    )

    fun beregnAvkortingMedNyttGrunnlag(
        nyttGrunnlag: AvkortingGrunnlag,
        behandlingstype: BehandlingType,
        virkningstidspunkt: Virkningstidspunkt,
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
        virkningstidspunkt: Virkningstidspunkt,
        beregning: Beregning
    ): Avkorting = if (behandlingstype == BehandlingType.FØRSTEGANGSBEHANDLING) {
        beregnAvkortingForstegangs(virkningstidspunkt, beregning)
    } else {
        beregnAvkortingRevurdering(virkningstidspunkt, beregning)
    }

    private fun beregnAvkortingForstegangs(
        virkningstidspunkt: Virkningstidspunkt,
        beregning: Beregning
    ): Avkorting {

        val ytelseFoerAvkorting = beregning.mapTilYtelseFoerAvkorting()

        val avkortingsperioder = AvkortingRegelkjoring.beregnInntektsavkorting(
            Periode(fom = virkningstidspunkt.dato, tom = null),
            avkortingGrunnlag
        )

        val beregnetAvkortetYtelse = AvkortingRegelkjoring.beregnAvkortetYtelse(
            virkningstidspunkt,
            ytelseFoerAvkorting,
            avkortingsperioder
        )

        return this.copy(
            aarsoppgjoer = Aarsoppgjoer(
                ytelseFoerAvkorting = ytelseFoerAvkorting,
                avkortingsperioder = avkortingsperioder,
                tidligereAvkortetYtelse = emptyList(),
                reberegnetAvkortetYtelse = emptyList(),
                restanse = null,
            ),
            avkortetYtelse = beregnetAvkortetYtelse
        )
    }

    private fun beregnAvkortingRevurdering(virkningstidspunkt: Virkningstidspunkt, beregning: Beregning): Avkorting {

        val ytelseFoerAvkorting =
            this.aarsoppgjoer.ytelseFoerAvkorting.leggTilNyeBeregninger(beregning, virkningstidspunkt.dato)

        val fraFoersteMaaned = Periode(fom = this.foersteMaanedDetteAar(), tom = null)
        val avkortingHeleAaret = AvkortingRegelkjoring.beregnInntektsavkorting(
            fraFoersteMaaned,
            avkortingGrunnlag = listOf(avkortingGrunnlag.last().copy(periode = fraFoersteMaaned))
        )

        val reberegnetYtelseFoerVirk = AvkortingRegelkjoring.beregnAvkortetYtelsePaaNytt(
            virkningstidspunkt,
            ytelseFoerAvkorting,
            avkortingHeleAaret
        )

        val restanse = AvkortingRegelkjoring.beregnRestanse(
            this.foersteMaanedDetteAar(),
            virkningstidspunkt,
            this.aarsoppgjoer.tidligereAvkortetYtelse,
            reberegnetYtelseFoerVirk,
        )

        val avkortetYtelseFraNyVirk = AvkortingRegelkjoring.beregnAvkortetYtelse(
            virkningstidspunkt,
            ytelseFoerAvkorting,
            avkortingHeleAaret,
            restanse
        )

        return this.copy(
            aarsoppgjoer = this.aarsoppgjoer.copy(
                ytelseFoerAvkorting = ytelseFoerAvkorting,
                avkortingsperioder = avkortingHeleAaret,
                reberegnetAvkortetYtelse = reberegnetYtelseFoerVirk,
                restanse = restanse
            ),
            avkortetYtelse = avkortetYtelseFraNyVirk
        )
    }

    private fun foersteMaanedDetteAar() = this.aarsoppgjoer.ytelseFoerAvkorting.first().periode.fom

    companion object {
        fun nyAvkorting(
            avkortingGrunnlag: List<AvkortingGrunnlag> = emptyList(),
            ytelseFoerAvkorting: List<YtelseFoerAvkorting> = emptyList(),
            tidligereAvkortetYtelse: List<AvkortetYtelse> = emptyList(),
            avkortetYtelse: List<AvkortetYtelse> = emptyList(),
        ) = Avkorting(
            avkortingGrunnlag = avkortingGrunnlag,
            aarsoppgjoer = Aarsoppgjoer(
                ytelseFoerAvkorting = ytelseFoerAvkorting,
                avkortingsperioder = emptyList(),
                tidligereAvkortetYtelse = tidligereAvkortetYtelse,
                reberegnetAvkortetYtelse = emptyList(),
                restanse = null,
            ),
            avkortetYtelse = avkortetYtelse,
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

data class Aarsoppgjoer(
    val ytelseFoerAvkorting: List<YtelseFoerAvkorting>,
    val avkortingsperioder: List<Avkortingsperiode>,
    val tidligereAvkortetYtelse: List<AvkortetYtelse>,
    val reberegnetAvkortetYtelse: List<AvkortetYtelse>,
    val restanse: Restanse?,
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
    val kilde: Grunnlagsopplysning.RegelKilde
)

data class Restanse(
    val id: UUID,
    val totalRestanse: Int,
    val fordeltRestanse: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode,
    val kilde: Grunnlagsopplysning.RegelKilde,
)

data class AvkortetYtelse(
    val id: UUID,
    val type: AvkortetYtelseType,
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
enum class AvkortetYtelseType { NY, TIDLIGERE, REBEREGNET }

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
    .filter { beregning.beregningId != it.beregningsreferanse }.map {
        when (it.periode.tom) {
            null -> it.copy(
                periode = Periode(fom = it.periode.fom, tom = virkningstidspunkt.minusMonths(1))
            )

            else -> it
        }
    } + beregning.mapTilYtelseFoerAvkorting()
