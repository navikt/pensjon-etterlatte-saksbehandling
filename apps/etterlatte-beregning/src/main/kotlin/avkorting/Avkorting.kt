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

        val ytelseEtterAvkortingAlleInntekter = mutableListOf<AvkortetYtelse>()
        this.avkortingGrunnlag.forEach { inntekt ->
            val periode = Periode(fom = this.foersteMaanedDetteAar(), tom = inntekt.periode.tom)

            val avkortinger = AvkortingRegelkjoring.beregnInntektsavkorting(
                periode = Periode(fom = this.foersteMaanedDetteAar(), tom = inntekt.periode.tom),
                avkortingGrunnlag = listOf(inntekt.copy(periode = periode))
            )

            val ytelseEtterAvkortingGjeldendeInntekt = AvkortingRegelkjoring.beregnAvkortetYtelse(
                periode = periode,
                ytelseFoerAvkorting = ytelseFoerAvkorting,
                avkortingsperioder = avkortinger,
                type = AvkortetYtelseType.REBEREGNET
            )

            val restanse = AvkortingRegelkjoring.beregnRestanse(
                this.foersteMaanedDetteAar(),
                inntekt.periode.fom,
                ytelseEtterAvkortingAlleInntekter,
                ytelseEtterAvkortingGjeldendeInntekt,
            )

            val ytelse = AvkortingRegelkjoring.beregnAvkortetYtelse(
                inntekt.periode,
                ytelseFoerAvkorting,
                avkortinger,
                restanse
            )
            ytelseEtterAvkortingAlleInntekter.addAll(ytelse) // TODO vil perioder overlappe?
        }

        val avkortetYtelseFraNyVirk =
            ytelseEtterAvkortingAlleInntekter.filter { it.periode.fom >= virkningstidspunkt.dato }.map {
                if (virkningstidspunkt.dato > it.periode.fom && (it.periode.tom == null || virkningstidspunkt.dato <= it.periode.tom)) {
                    it.copy(periode = Periode(fom = virkningstidspunkt.dato, tom = it.periode.tom))
                } else {
                    it
                }
            }

        return this.copy(
            aarsoppgjoer = this.aarsoppgjoer.copy(
                ytelseFoerAvkorting = ytelseFoerAvkorting,
                //avkortingsperioder = avkortingerAlleInntekter,
                // TODO EY-2523 - avkortinger siste inntekt - legge til typefelt (samme som avkortetYtelse?)
                //tidligereAvkortetYtelseReberegnet = ytelseEtterAvkortingGjeldendeInntekt,
                //restanse = restanse TODO restanse per avkortet ytelse?
            ),
            avkortetYtelse = avkortetYtelseFraNyVirk
        )
    }

    fun beregnEnkeltPeriode(
        avkortetYtelseId: UUID,
        manuellRestanse: Int,
        saksbehandler: String
    ): Avkorting {
        val skalReberegnes =
            avkortetYtelse.find { it.id == avkortetYtelseId } ?: throw Exception("Finner ikke avkoret ytelse")

        val reberegnetYtelseIPeriode = AvkortingRegelkjoring.beregnAvkortetYtelse(
            skalReberegnes.periode,
            aarsoppgjoer.ytelseFoerAvkorting,
            aarsoppgjoer.avkortingsperioder,
            Restanse( // TODO EY-2523 - Hva gjør vi med enkeltredigering her? Det må jo lagres..
                id = UUID.randomUUID(),
                totalRestanse = 0,
                fordeltRestanse = manuellRestanse,
                tidspunkt = Tidspunkt.now(),
                kilde = Grunnlagsopplysning.Saksbehandler.create(saksbehandler),
                regelResultat = null
            )
        )
        val avkortetYtelse = avkortetYtelse.filter { it.id != avkortetYtelseId } + reberegnetYtelseIPeriode

        return this.copy(
            avkortetYtelse = avkortetYtelse,
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
    // TODO avkortingsperioderSisteInntekt / reberegnet
    val tidligereAvkortetYtelse: List<AvkortetYtelse> = emptyList(), // TODO endre til alle inntekter og siste inntekt
    val tidligereAvkortetYtelseReberegnet: List<AvkortetYtelse> = emptyList(), // TODO endre til alle inntekter og siste inntekt
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

// TODO EY-2523 unittest!
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
