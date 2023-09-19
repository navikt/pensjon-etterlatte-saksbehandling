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
    val avkortetYtelseFraVirkningstidspunkt: List<AvkortetYtelse> = emptyList(),
    val avkortetYtelseForrigeVedtak: List<AvkortetYtelse> = emptyList()
) {

    /*
    * Årsoppgjør inneholder ytelsen sine perioder for hele år og for behandlinger/vedtak er det kun
    * virknigstidspunkt og fremover som er relevant.
    */
    fun medYtelseFraOgMedVirkningstidspunkt(
        virkningstidspunkt: YearMonth,
        forrigeAvkorting: Avkorting? = null
    ): Avkorting = this.copy(
        avkortetYtelseFraVirkningstidspunkt = aarsoppgjoer.avkortetYtelseAar
            .filter { it.periode.tom == null || virkningstidspunkt <= it.periode.tom }
            .map {
                if (virkningstidspunkt > it.periode.fom && (it.periode.tom == null || virkningstidspunkt <= it.periode.tom)) {
                    it.copy(periode = Periode(fom = virkningstidspunkt, tom = it.periode.tom))
                } else {
                    it
                }
            },
        avkortetYtelseForrigeVedtak = forrigeAvkorting?.aarsoppgjoer?.avkortetYtelseAar ?: emptyList()
    )

    /*
     * Skal kun benyttes ved opprettelse av ny avkorting ved revurdering.
     */
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
        behandlingstype: BehandlingType,
        beregning: Beregning
    ) = if (behandlingstype == BehandlingType.FØRSTEGANGSBEHANDLING) {
        oppdaterMedInntektsgrunnlag(nyttGrunnlag).beregnAvkortingForstegangs(beregning)
    } else {
        oppdaterMedInntektsgrunnlag(nyttGrunnlag).beregnAvkortingRevurdering(beregning)
    }

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

    private fun beregnAvkortingForstegangs(beregning: Beregning): Avkorting {
        val ytelseFoerAvkorting = beregning.mapTilYtelseFoerAvkorting()
        val grunnlag = aarsoppgjoer.inntektsavkorting.first().grunnlag

        val avkortingsperioder = AvkortingRegelkjoring.beregnInntektsavkorting(
            periode = grunnlag.periode,
            avkortingGrunnlag = listOf(grunnlag)
        )

        val beregnetAvkortetYtelse = AvkortingRegelkjoring.beregnAvkortetYtelse(
            periode = grunnlag.periode,
            ytelseFoerAvkorting = ytelseFoerAvkorting,
            avkortingsperioder = avkortingsperioder,
            type = AvkortetYtelseType.FORVENTET_INNTEKT
        )

        return this.copy(
            aarsoppgjoer = this.aarsoppgjoer.copy(
                ytelseFoerAvkorting = ytelseFoerAvkorting,
                inntektsavkorting = this.aarsoppgjoer.inntektsavkorting.map {
                    it.copy(
                        avkortingsperioder = avkortingsperioder,
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

    fun beregnAvkortingRevurdering(beregning: Beregning): Avkorting {
        val ytelseFoerAvkorting = this.aarsoppgjoer.ytelseFoerAvkorting.leggTilNyeBeregninger(beregning)

        val reberegnetInntektsavkorting = this.aarsoppgjoer.inntektsavkorting.map { inntektsavkorting ->
            val periode = Periode(fom = this.foersteMaanedDetteAar(), tom = inntektsavkorting.grunnlag.periode.tom)

            val avkortinger = AvkortingRegelkjoring.beregnInntektsavkorting(
                periode = periode,
                avkortingGrunnlag = listOf(inntektsavkorting.grunnlag.copy(periode = periode))
            )

            val avkortetYtelseForventetInntekt = if (this.aarsoppgjoer.inntektsavkorting.size > 1) {
                AvkortingRegelkjoring.beregnAvkortetYtelse(
                    periode = periode,
                    ytelseFoerAvkorting = ytelseFoerAvkorting,
                    avkortingsperioder = avkortinger,
                    type = AvkortetYtelseType.FORVENTET_INNTEKT
                )
            } else emptyList()

            inntektsavkorting.copy(
                avkortingsperioder = avkortinger,
                avkortetYtelseForventetInntekt = avkortetYtelseForventetInntekt.map {
                    it.copy(inntektsgrunnlag = inntektsavkorting.grunnlag.id)
                }
            )
        }

        val avkortetYtelse = if (this.aarsoppgjoer.inntektsavkorting.size > 1) {
            beregnAvkortetYtelseMedRestanse(ytelseFoerAvkorting, reberegnetInntektsavkorting)
        } else {
            reberegnetInntektsavkorting.first().let {
                AvkortingRegelkjoring.beregnAvkortetYtelse(
                    periode = it.grunnlag.periode,
                    ytelseFoerAvkorting = ytelseFoerAvkorting,
                    avkortingsperioder = it.avkortingsperioder,
                    type = AvkortetYtelseType.AARSOPPGJOER,
                    restanse = null,
                )
            }
        }

        return this.copy(
            aarsoppgjoer = this.aarsoppgjoer.copy(
                ytelseFoerAvkorting = ytelseFoerAvkorting,
                inntektsavkorting = reberegnetInntektsavkorting,
                avkortetYtelseAar = avkortetYtelse
            ),
        )
    }

    /**
     * Finner avkortet ytelse med opparbeidet [Restanse]
     * Opparbeidet restanse beregnes ved å sammenligne samtlige forventa inntektsavkortinger med alle måneder frem til
     * ny oppgitt forventet inntekt.
     * For hver forventet inntekt som sammenlignes så akkumuleres det mer eller mindre restanse.
     */
    private fun beregnAvkortetYtelseMedRestanse(
        ytelseFoerAvkorting: List<YtelseFoerAvkorting>,
        reberegnetInntektsavkorting: List<Inntektsavkorting>
    ): List<AvkortetYtelse> {
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
        return avkortetYtelseMedAllForventetInntekt
    }

    private fun foersteMaanedDetteAar() = this.aarsoppgjoer.ytelseFoerAvkorting.first().periode.fom

}

/**
 * Kan være forventet årsinntekt oppgitt av bruker eller faktisk årsinntekt etter skatteoppgjør.
 */
data class AvkortingGrunnlag(
    val id: UUID,
    val periode: Periode,
    val aarsinntekt: Int,
    val fratrekkInnAar: Int,
    val relevanteMaanederInnAar: Int,
    val inntektUtland: Int,
    val fratrekkInnAarUtland: Int,
    val spesifikasjon: String,
    val kilde: Grunnlagsopplysning.Saksbehandler
)

data class Aarsoppgjoer(
    val ytelseFoerAvkorting: List<YtelseFoerAvkorting> = emptyList(),
    val inntektsavkorting: List<Inntektsavkorting> = emptyList(),
    val avkortetYtelseAar: List<AvkortetYtelse> = emptyList()
)

/**
 * [avkortingsperioder] utregnet basert på en årsinntekt ([grunnlag]).
 *
 * [avkortetYtelseForventetInntekt] - Benyttes hvis forventet årsinntekt endrer seg i løpet av året for å finne
 * restansen (se [Avkorting.beregnAvkortetYtelseMedRestanse]). Vil da inneholde ytelse etter avkorting slik
 * den ville vært med denne årsinntekten.
 */
data class Inntektsavkorting(
    val grunnlag: AvkortingGrunnlag,
    val avkortingsperioder: List<Avkortingsperiode> = emptyList(),
    val avkortetYtelseForventetInntekt: List<AvkortetYtelse> = emptyList()
)

/**
 * Beregnet ytelse (ytelse før avkorting / [Beregning]) persisteres for hele år for å kunne
 * beregne ytelse etter avkorting for et helt år av gangen. Det er nødvendig for [Restanse] og etteroppgjør.
 * (se [leggTilNyeBeregninger]).
 */
data class YtelseFoerAvkorting(
    val beregning: Int,
    val periode: Periode,
    val beregningsreferanse: UUID
)

/**
 * Utregnet avkortingsbeløp basert på årsinntekt som benyttes til å avkorte ytelse
 */
data class Avkortingsperiode(
    val id: UUID,
    val periode: Periode,
    val avkorting: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode,
    val kilde: Grunnlagsopplysning.RegelKilde,
    val inntektsgrunnlag: UUID
)

/**
 * Hvis forventet årsinntekt har endret seg i løpet av året vil det ha blitt avkortet for lite eller for mye.
 * Dette avviket blir "restanse" som fordeles over gjenværende måneder av året for å minimere avvik på etteroppgjøret.
 * (se [Avkorting.beregnAvkortetYtelseMedRestanse]).
 */
data class Restanse(
    val id: UUID,
    val totalRestanse: Int,
    val fordeltRestanse: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode?,
    val kilde: Grunnlagsopplysning.Kilde,
)

/**
 * Ytelsen etter avkortingsbeløp ([Avkortingsperiode]) er blit trukket ifra [YtelseFoerAvkorting].
 */
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

/**
 * [FORVENTET_INNTEKT] - Ytelse avkortet etter det som var brukeroppgitt forventet årsinntekt i perioden
 *
 * [AARSOPPGJOER] - Iverksatte perioder. Inneholder restanse hvis forventet inntekt endrer seg i løpet av året
 *
 * [ETTEROPPJOER] - TODO ikke enda implementert
 */
enum class AvkortetYtelseType { FORVENTET_INNTEKT, AARSOPPGJOER, ETTEROPPJOER }

fun Beregning.mapTilYtelseFoerAvkorting() = beregningsperioder.map {
    YtelseFoerAvkorting(
        beregning = it.utbetaltBeloep,
        periode = Periode(it.datoFOM, it.datoTOM),
        beregningsreferanse = this.beregningId
    )
}

/*
* Når det kommer nye beregninger skal det legges til eller erstatte de eksisterende i samme periode.
* Eksisterende perioder som er før nye beregninger skal beholdes.
*
* Overlapp mellom eksisterende og nye perioder håndteres ved å sette eksisterende til og med til måned før
* første nye fra og med.
*
* NB! Når siste eksisterende og første nye overlapper kan det føre til at to perioder etter hverandre har
* helt like verdier. Dette fordi vi ikke sammenligner innhold og slår sammen men kun avslutter eksisterende
* periode.
*/
private fun List<YtelseFoerAvkorting>.leggTilNyeBeregninger(beregning: Beregning): List<YtelseFoerAvkorting> {
    val nyYtelseFoerAvkorting = beregning.mapTilYtelseFoerAvkorting()
    val fraOgMedNyYtelse = nyYtelseFoerAvkorting.first().periode.fom

    val eksisterendeFremTilNye = filter { it.periode.fom < fraOgMedNyYtelse }
        .filter { beregning.beregningId != it.beregningsreferanse }

    val eksisterendeAvrundetPerioder = eksisterendeFremTilNye.map { ytelseFoerAvkorting ->
        if (ytelseFoerAvkorting.periode.tom == null
            || fraOgMedNyYtelse <= ytelseFoerAvkorting.periode.tom
        ) {
            ytelseFoerAvkorting.copy(
                periode = Periode(
                    fom = ytelseFoerAvkorting.periode.fom,
                    tom = fraOgMedNyYtelse.minusMonths(1)
                )
            )
        } else {
            ytelseFoerAvkorting
        }
    }

    return eksisterendeAvrundetPerioder + nyYtelseFoerAvkorting
}