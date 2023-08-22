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
    val avkortingGrunnlag: List<AvkortingGrunnlag> = emptyList(),
    val aarsoppgjoer: Aarsoppgjoer = Aarsoppgjoer(),
    val avkortetYtelse: List<AvkortetYtelse> = emptyList()
) {

    fun kopierAvkorting(iverksettelse: YearMonth): Avkorting = Avkorting(
        avkortingGrunnlag = avkortingGrunnlag.map { it.copy(id = UUID.randomUUID()) },
        aarsoppgjoer = Aarsoppgjoer(
            // TODO EY-2523 - RYDD! Og TEST!
            tidligereAvkortetYtelse = aarsoppgjoer.tidligereAvkortetYtelse
                .filter { it.periode.fom < avkortetYtelse[0].periode.fom }
                .map { it.copy(id = UUID.randomUUID()) } + avkortetYtelse.map {
                when (it.periode.tom) {
                    null -> it.copy(
                        id = UUID.randomUUID(),
                        type = AvkortetYtelseType.TIDLIGERE,
                        //periode = Periode(fom = it.periode.fom, tom = iverksettelse.minusMonths(1)) // TODO ??
                    )

                    else -> it.copy(
                        id = UUID.randomUUID(),
                        type = AvkortetYtelseType.TIDLIGERE
                    )
                }
            },
            ytelseFoerAvkorting = aarsoppgjoer.ytelseFoerAvkorting.map { it },
            tidligereAvkortetYtelseReberegnet = listOf(), // TODO test
            restanse = null // TODO test
        ),
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
        // TODO EY-2556
        if (virkningstidspunkt.dato > YearMonth.now()) {
            beregnAvkortingRevurdering(virkningstidspunkt, beregning)
        } else {
            beregnAvkortingTilbakeITid(virkningstidspunkt, beregning)
        }
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
            Periode(fom = virkningstidspunkt.dato, tom = null),
            ytelseFoerAvkorting,
            avkortingsperioder
        )

        return this.copy(
            aarsoppgjoer = Aarsoppgjoer(
                ytelseFoerAvkorting = ytelseFoerAvkorting,
                avkortingsperioder = avkortingsperioder,
                tidligereAvkortetYtelse = emptyList(),
                tidligereAvkortetYtelseReberegnet = emptyList(),
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


        val periodeFoerVirk = Periode(fom = ytelseFoerAvkorting.first().periode.fom, tom = virkningstidspunkt.dato.minusMonths(1))
        val reberegnetYtelseFoerVirk = AvkortingRegelkjoring.beregnAvkortetYtelse(
            periodeFoerVirk,
            ytelseFoerAvkorting,
            avkortingHeleAaret,
            type = AvkortetYtelseType.REBEREGNET
        )

        val restanse = AvkortingRegelkjoring.beregnRestanse(
            this.foersteMaanedDetteAar(),
            virkningstidspunkt,
            this.aarsoppgjoer.tidligereAvkortetYtelse,
            reberegnetYtelseFoerVirk,
        )

        val avkortetYtelseFraNyVirk = AvkortingRegelkjoring.beregnAvkortetYtelse(
            Periode(fom = virkningstidspunkt.dato, tom = null),
            ytelseFoerAvkorting,
            avkortingHeleAaret,
            restanse
        )

        return this.copy(
            aarsoppgjoer = this.aarsoppgjoer.copy(
                ytelseFoerAvkorting = ytelseFoerAvkorting,
                avkortingsperioder = avkortingHeleAaret,
                tidligereAvkortetYtelseReberegnet = reberegnetYtelseFoerVirk,
                restanse = restanse
            ),
            avkortetYtelse = avkortetYtelseFraNyVirk
        )
    }

    private fun beregnAvkortingTilbakeITid(virkningstidspunkt: Virkningstidspunkt, beregning: Beregning): Avkorting {

        val ytelseFoerAvkorting =
            this.aarsoppgjoer.ytelseFoerAvkorting.leggTilNyeBeregninger(beregning, virkningstidspunkt.dato)

        val avkortinger = AvkortingRegelkjoring.beregnInntektsavkorting(
            Periode(fom = this.foersteMaanedDetteAar(), tom = null),
            avkortingGrunnlag = this.avkortingGrunnlag
        )

        val avkortetYtelseTilbakeITid = AvkortingRegelkjoring.beregnAvkortetYtelse(
            Periode(fom = this.foersteMaanedDetteAar(), tom = null),
            ytelseFoerAvkorting,
            avkortinger
        )

        return this.copy(
            aarsoppgjoer = this.aarsoppgjoer.copy(
                ytelseFoerAvkorting = ytelseFoerAvkorting,
                avkortingsperioder = avkortinger,
            ),
            avkortetYtelse = avkortetYtelseTilbakeITid
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
    val avkortingsperioder: List<Avkortingsperiode> = emptyList(),
    val tidligereAvkortetYtelse: List<AvkortetYtelse> = emptyList(),
    val tidligereAvkortetYtelseReberegnet: List<AvkortetYtelse> = emptyList(),
    val restanse: Restanse? = null,
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
    val regelResultat: JsonNode?,
    val kilde: Grunnlagsopplysning.RegelKilde
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

enum class AvkortetYtelseType { NY, TIDLIGERE, REBEREGNET } // TODO ETTEROPPGJOER

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
