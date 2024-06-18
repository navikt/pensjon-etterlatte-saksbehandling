package no.nav.etterlatte.avkorting

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.avkorting.AvkortetYtelseType.AARSOPPGJOER
import no.nav.etterlatte.avkorting.AvkortetYtelseType.ETTEROPPJOER
import no.nav.etterlatte.avkorting.AvkortetYtelseType.FORVENTET_INNTEKT
import no.nav.etterlatte.beregning.Beregning
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.beregning.SanksjonertYtelse
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.sanksjon.Sanksjon
import java.time.YearMonth
import java.util.UUID

data class Avkorting(
    val aarsoppgjoer: List<Aarsoppgjoer> = emptyList(),
    val avkortetYtelseFraVirkningstidspunkt: List<AvkortetYtelse> = emptyList(),
    val avkortetYtelseForrigeVedtak: List<AvkortetYtelse> = emptyList(),
) {
    /*
     * avkortetYtelseFraVirkningstidspunkt - Årsoppgjør inneholder ytelsen sine perioder for hele år
     * og for behandlinger/vedtak er det kun virknigstidspunkt og fremover som er relevant.
     *
     * avkortetYtelseForrigeVedtak - brukes til å sammenligne med beløper til nye beregna perioder under behandlingen
     */
    fun medYtelseFraOgMedVirkningstidspunkt(
        virkningstidspunkt: YearMonth,
        forrigeAvkorting: Avkorting? = null,
    ): Avkorting =
        this.copy(
            avkortetYtelseFraVirkningstidspunkt =
                aarsoppgjoer.filter { virkningstidspunkt.year <= it.aar }.flatMap { aarsoppgjoer ->
                    aarsoppgjoer.avkortetYtelseAar
                        .filter { it.periode.tom == null || virkningstidspunkt <= it.periode.tom }
                        .map {
                            if (virkningstidspunkt > it.periode.fom && (it.periode.tom == null || virkningstidspunkt <= it.periode.tom)) {
                                it.copy(periode = Periode(fom = virkningstidspunkt, tom = it.periode.tom))
                            } else {
                                it
                            }
                        }
                },
            avkortetYtelseForrigeVedtak =
                forrigeAvkorting?.aarsoppgjoer?.flatMap {
                    it.avkortetYtelseAar
                } ?: emptyList(),
        )

    /*
     * Skal kun benyttes ved opprettelse av ny avkorting ved revurdering.
     */
    fun kopierAvkorting(): Avkorting =
        Avkorting(
            aarsoppgjoer =
                aarsoppgjoer.map {
                    it.copy(
                        id = UUID.randomUUID(),
                        inntektsavkorting =
                            it.inntektsavkorting.map { inntektsavkorting ->
                                inntektsavkorting.copy(grunnlag = inntektsavkorting.grunnlag.copy(id = UUID.randomUUID()))
                            },
                    )
                },
        )

    fun beregnAvkortingMedNyttGrunnlag(
        nyttGrunnlag: AvkortingGrunnlagLagreDto,
        behandlingstype: BehandlingType,
        virkningstidspunkt: YearMonth,
        bruker: BrukerTokenInfo,
        beregning: Beregning,
        sanksjoner: List<Sanksjon>,
    ) = if (behandlingstype == BehandlingType.FØRSTEGANGSBEHANDLING) {
        oppdaterMedInntektsgrunnlag(nyttGrunnlag, virkningstidspunkt, true, bruker).beregnAvkortingForstegangs(
            beregning,
            sanksjoner,
        )
    } else {
        // TODO parameter innvilgelse vil ikke være tilstrekkelig når vi skal revurdere for å endre første virk..
        oppdaterMedInntektsgrunnlag(nyttGrunnlag, virkningstidspunkt, false, bruker).beregnAvkortingRevurdering(
            beregning,
            sanksjoner,
        )
    }

    fun oppdaterMedInntektsgrunnlag(
        nyttGrunnlag: AvkortingGrunnlagLagreDto,
        virkningstidspunkt: YearMonth,
        innvilgelse: Boolean,
        bruker: BrukerTokenInfo,
    ): Avkorting {
        // TODO kreve at det er inneværende år?
        val aarsoppgjoer = hentAarsoppgjoer(virkningstidspunkt, innvilgelse)
        val oppdatert =
            aarsoppgjoer.inntektsavkorting
                // Fjerner hvis det finnes fra før for å erstatte/redigere
                .filter { it.grunnlag.id != nyttGrunnlag.id }
                .map { it.lukkSisteInntektsperiode(virkningstidspunkt) } +
                listOf(
                    Inntektsavkorting(
                        grunnlag =
                            AvkortingGrunnlag(
                                id = nyttGrunnlag.id,
                                periode = Periode(fom = virkningstidspunkt, tom = null),
                                aarsinntekt = nyttGrunnlag.aarsinntekt,
                                fratrekkInnAar = nyttGrunnlag.fratrekkInnAar,
                                inntektUtland = nyttGrunnlag.inntektUtland,
                                fratrekkInnAarUtland = nyttGrunnlag.fratrekkInnAarUtland,
                                spesifikasjon = nyttGrunnlag.spesifikasjon,
                                kilde = Grunnlagsopplysning.Saksbehandler(bruker.ident(), Tidspunkt.now()),
                            ),
                    ),
                )

        val oppdatertAarsoppjoer =
            aarsoppgjoer.copy(
                inntektsavkorting = oppdatert,
            )
        return this.copy(
            aarsoppgjoer = erstattAarsoppgjoer(oppdatertAarsoppjoer),
        )
    }

    private fun beregnAvkortingForstegangs(
        beregning: Beregning,
        sanksjoner: List<Sanksjon>,
    ): Avkorting {
        val aarsoppgjoer = aarsoppgjoer.single()
        val ytelseFoerAvkorting = beregning.mapTilYtelseFoerAvkorting()
        val grunnlag = aarsoppgjoer.inntektsavkorting.first().grunnlag

        val avkortingsperioder =
            AvkortingRegelkjoring.beregnInntektsavkorting(
                periode = grunnlag.periode,
                avkortingGrunnlag = grunnlag,
                aarsoppgjoer.forventaInnvilgaMaaneder,
            )

        val avkortetYtelseAar =
            AvkortingRegelkjoring.beregnAvkortetYtelse(
                periode = grunnlag.periode,
                ytelseFoerAvkorting = ytelseFoerAvkorting,
                avkortingsperioder = avkortingsperioder,
                type = AARSOPPGJOER,
                sanksjoner = sanksjoner,
            )

        val oppdatertAarsoppgjoer =
            aarsoppgjoer.copy(
                ytelseFoerAvkorting = ytelseFoerAvkorting,
                inntektsavkorting =
                    aarsoppgjoer.inntektsavkorting.map { inntektsavkorting ->
                        inntektsavkorting.copy(
                            avkortingsperioder = avkortingsperioder,
                        )
                    },
                avkortetYtelseAar = avkortetYtelseAar,
            )
        return this.copy(
            aarsoppgjoer = erstattAarsoppgjoer(oppdatertAarsoppgjoer),
        )
    }

    fun beregnAvkortingRevurdering(
        beregning: Beregning,
        sanksjoner: List<Sanksjon>,
    ): Avkorting {
        val virkningstidspunktAar =
            beregning.beregningsperioder
                .first()
                .datoFOM.year

        val oppdaterteOppgjoer =
            this.aarsoppgjoer.map { aarsoppgjoer ->

                val ytelseFoerAvkorting =
                    if (aarsoppgjoer.aar >= virkningstidspunktAar) {
                        aarsoppgjoer.ytelseFoerAvkorting.leggTilNyeBeregninger(beregning)
                    } else {
                        aarsoppgjoer.ytelseFoerAvkorting
                    }

                val reberegnetInntektsavkorting =
                    aarsoppgjoer.inntektsavkorting.map { inntektsavkorting ->
                        val periode =
                            Periode(
                                fom = aarsoppgjoer.foersteInnvilgedeMaaned(),
                                tom = inntektsavkorting.grunnlag.periode.tom,
                            )

                        val avkortinger =
                            AvkortingRegelkjoring.beregnInntektsavkorting(
                                periode = periode,
                                avkortingGrunnlag = inntektsavkorting.grunnlag.copy(periode = periode),
                                aarsoppgjoer.forventaInnvilgaMaaneder,
                            )

                        val avkortetYtelseForventetInntekt =
                            if (aarsoppgjoer.inntektsavkorting.size > 1) {
                                AvkortingRegelkjoring.beregnAvkortetYtelse(
                                    periode = periode,
                                    ytelseFoerAvkorting = ytelseFoerAvkorting,
                                    avkortingsperioder = avkortinger,
                                    type = FORVENTET_INNTEKT,
                                    restanse = null,
                                    sanksjoner = emptyList(),
                                )
                            } else {
                                emptyList()
                            }

                        inntektsavkorting.copy(
                            avkortingsperioder = avkortinger,
                            avkortetYtelseForventetInntekt =
                                avkortetYtelseForventetInntekt.map {
                                    it.copy(inntektsgrunnlag = inntektsavkorting.grunnlag.id)
                                },
                        )
                    }

                val avkortetYtelse =
                    if (aarsoppgjoer.inntektsavkorting.size > 1) {
                        beregnAvkortetYtelseMedRestanse(
                            aarsoppgjoer,
                            ytelseFoerAvkorting,
                            reberegnetInntektsavkorting,
                            sanksjoner,
                        )
                    } else {
                        reberegnetInntektsavkorting.first().let {
                            AvkortingRegelkjoring.beregnAvkortetYtelse(
                                periode = it.grunnlag.periode,
                                ytelseFoerAvkorting = ytelseFoerAvkorting,
                                avkortingsperioder = it.avkortingsperioder,
                                type = AARSOPPGJOER,
                                restanse = null,
                                sanksjoner = sanksjoner,
                            )
                        }
                    }

                aarsoppgjoer.copy(
                    ytelseFoerAvkorting = ytelseFoerAvkorting,
                    inntektsavkorting = reberegnetInntektsavkorting,
                    avkortetYtelseAar = avkortetYtelse,
                )
            }

        return this.copy(aarsoppgjoer = oppdaterteOppgjoer)
    }

    /**
     * Finner avkortet ytelse med opparbeidet [Restanse]
     * Opparbeidet restanse beregnes ved å sammenligne samtlige forventa inntektsavkortinger med alle måneder frem til
     * ny oppgitt forventet inntekt.
     * For hver forventet inntekt som sammenlignes så akkumuleres det mer eller mindre restanse.
     */
    private fun beregnAvkortetYtelseMedRestanse(
        aarsoppgjoer: Aarsoppgjoer,
        ytelseFoerAvkorting: List<YtelseFoerAvkorting>,
        reberegnetInntektsavkorting: List<Inntektsavkorting>,
        sanksjoner: List<Sanksjon>,
    ): List<AvkortetYtelse> {
        val sorterteSanksjonerInnenforAarsoppgjoer =
            aarsoppgjoer
                .sanksjonerInnenforAarsoppjoer(sanksjoner)
                .sortedBy { it.fom }
        val avkortetYtelseMedAllForventetInntekt = mutableListOf<AvkortetYtelse>()
        reberegnetInntektsavkorting.forEachIndexed { i, inntektsavkorting ->
            // Kun de sanksjonene som er lagt inn fom < denne inntektsavkortingen er tidligere beregnet med
            // så hvis vi ikke ekskluderer senere sanksjoner endrer vi restanseutregning tilbake i tid og
            // omfordeler i perioder før sanksjonen blir lagt inn
            val foersteFomDenneInntektsavkortingen = inntektsavkorting.avkortingsperioder.minOf { it.periode.fom }
            val kjenteSanksjonerForInntektsavkorting =
                sorterteSanksjonerInnenforAarsoppgjoer.filter {
                    it.fom < foersteFomDenneInntektsavkortingen
                }

            val restanse =
                when (i) {
                    0 -> null
                    else ->
                        AvkortingRegelkjoring.beregnRestanse(
                            aarsoppgjoer.foersteInnvilgedeMaaned(),
                            inntektsavkorting,
                            avkortetYtelseMedAllForventetInntekt,
                            kjenteSanksjonerForInntektsavkorting,
                        )
                }
            val ytelse =
                AvkortingRegelkjoring.beregnAvkortetYtelse(
                    periode = inntektsavkorting.grunnlag.periode,
                    ytelseFoerAvkorting = ytelseFoerAvkorting,
                    avkortingsperioder = inntektsavkorting.avkortingsperioder,
                    type = AARSOPPGJOER,
                    restanse = restanse,
                    sanksjoner = kjenteSanksjonerForInntektsavkorting,
                )
            avkortetYtelseMedAllForventetInntekt.addAll(ytelse)
        }
        val senesteInntektsjusteringFom =
            reberegnetInntektsavkorting.maxOf { inntektsavkorting -> inntektsavkorting.avkortingsperioder.minOf { it.periode.fom } }
        val senesteSanksjonFom = sorterteSanksjonerInnenforAarsoppgjoer.maxOfOrNull { it.fom }

        // Hvis vi har sanksjoner som ikke er tatt høyde for i beregningen av ytelse opp mot restanse over, må vi
        // ta høyde for de til slutt
        if (senesteSanksjonFom != null && senesteSanksjonFom >= senesteInntektsjusteringFom) {
            val restanse =
                AvkortingRegelkjoring.beregnRestanse(
                    aarsoppgjoer.foersteInnvilgedeMaaned(),
                    reberegnetInntektsavkorting.last(),
                    avkortetYtelseMedAllForventetInntekt,
                    sorterteSanksjonerInnenforAarsoppgjoer,
                )
            // Ytelse etter avkorting må reberegnes fra første sanksjon som ikke er "sett" i tidlegere beregninger
            val tidligsteFomIkkeBeregnetSanksjon =
                requireNotNull(sorterteSanksjonerInnenforAarsoppgjoer.firstOrNull { it.fom >= senesteInntektsjusteringFom }?.fom) {
                    "Fant tidligere at vi har en sanksjon som er etter siste inntektsjustering, men finner ingen nå"
                }
            val ytelseMedAlleSanksjoner =
                AvkortingRegelkjoring.beregnAvkortetYtelse(
                    periode = Periode(fom = tidligsteFomIkkeBeregnetSanksjon, tom = null),
                    ytelseFoerAvkorting = ytelseFoerAvkorting,
                    avkortingsperioder = reberegnetInntektsavkorting.last().avkortingsperioder,
                    type = AARSOPPGJOER,
                    sanksjoner = sorterteSanksjonerInnenforAarsoppgjoer,
                    restanse = restanse,
                )

            // Slår sammen ytelsesperiodene før siste beregning med sanksjon med siste beregning av sanksjon
            val perioderSomBeholdes =
                avkortetYtelseMedAllForventetInntekt.takeWhile { it.periode.fom < tidligsteFomIkkeBeregnetSanksjon }
            val lukketSistePeriodeFoerSanksjonsberegningen =
                perioderSomBeholdes.map {
                    if (it.periode.tom != null) {
                        it
                    } else {
                        it.copy(periode = it.periode.copy(tom = tidligsteFomIkkeBeregnetSanksjon.minusMonths(1)))
                    }
                }
            return lukketSistePeriodeFoerSanksjonsberegningen + ytelseMedAlleSanksjoner
        }

        return avkortetYtelseMedAllForventetInntekt
    }

    /*
     * Hvilket årsoppgjør som er relevant basers på virkningstidspunkt.
     * Hvis det ikke finnes et fra før på virkningstidspunkt opprettes et nytt.
     *
     * Ved innvilgelse/førstegangsbehandling så skal måneder før virkningsitdspunkt trekkes i fra
     * forventa innvilgede måneder.
     */
    private fun hentAarsoppgjoer(
        virkningstidspunkt: YearMonth,
        innvilgelse: Boolean,
    ): Aarsoppgjoer {
        val funnet = aarsoppgjoer.find { it.aar == virkningstidspunkt.year }
        return funnet ?: Aarsoppgjoer(
            id = UUID.randomUUID(),
            aar = virkningstidspunkt.year,
            forventaInnvilgaMaaneder = if (innvilgelse) (12 - virkningstidspunkt.monthValue + 1) else 12,
        )
    }

    private fun erstattAarsoppgjoer(nytt: Aarsoppgjoer): List<Aarsoppgjoer> {
        if (aarsoppgjoer.any { it.aar == nytt.aar }) {
            return aarsoppgjoer.map { if (it.aar == nytt.aar) nytt else it }
        }
        return aarsoppgjoer + listOf(nytt)
    }
}

/**
 * Kan være forventet årsinntekt oppgitt av bruker eller faktisk årsinntekt etter skatteoppgjør.
 */
data class AvkortingGrunnlag(
    val id: UUID,
    val periode: Periode,
    val aarsinntekt: Int,
    val fratrekkInnAar: Int,
    val inntektUtland: Int,
    val fratrekkInnAarUtland: Int,
    val spesifikasjon: String,
    val kilde: Grunnlagsopplysning.Saksbehandler,
)

data class Aarsoppgjoer(
    val id: UUID,
    val aar: Int,
    val forventaInnvilgaMaaneder: Int,
    val ytelseFoerAvkorting: List<YtelseFoerAvkorting> = emptyList(),
    val inntektsavkorting: List<Inntektsavkorting> = emptyList(),
    val avkortetYtelseAar: List<AvkortetYtelse> = emptyList(),
) {

    fun foersteInnvilgedeMaaned(): YearMonth = YearMonth.of(aar, 12 - forventaInnvilgaMaaneder + 1)

    /**
     * Gir kun de sanksjonene som har en periode som overlapper med dette årsoppgjøret.
     *
     * En sanksjon overlapper hvis:
     *  1. fra og med er i dette året
     *  2. fra og med er før dette året og til og med er ikke satt, eller dette året eller større
     *
     * @param sanksjoner - alle sanksjoner i ytelsen
     *
     * @return sanksjoner innenfor årsoppgjøres
     */
    fun sanksjonerInnenforAarsoppjoer(sanksjoner: List<Sanksjon>): List<Sanksjon> =
        sanksjoner.filter { sanksjon ->
            val erFomDetteAaret = sanksjon.fom.year == aar
            val overlapperTom = sanksjon.fom.year <= aar && (sanksjon.tom == null || sanksjon.tom.year >= aar)
            erFomDetteAaret || overlapperTom
        }
}

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
    val avkortetYtelseForventetInntekt: List<AvkortetYtelse> = emptyList(),
) {
    fun lukkSisteInntektsperiode(virkningstidspunkt: YearMonth) =
        when (grunnlag.periode.tom) {
            null ->
                copy(
                    grunnlag =
                        grunnlag.copy(
                            periode =
                                Periode(
                                    fom = grunnlag.periode.fom,
                                    tom = virkningstidspunkt.minusMonths(1),
                                ),
                        ),
                )

            else -> this
        }
}

/**
 * Beregnet ytelse (ytelse før avkorting / [Beregning]) persisteres for hele år for å kunne
 * beregne ytelse etter avkorting for et helt år av gangen. Det er nødvendig for [Restanse] og etteroppgjør.
 * (se [leggTilNyeBeregninger]).
 */
data class YtelseFoerAvkorting(
    val beregning: Int,
    val periode: Periode,
    val beregningsreferanse: UUID,
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
    val inntektsgrunnlag: UUID,
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
    val inntektsgrunnlag: UUID? = null,
    val sanksjon: SanksjonertYtelse? = null,
)

/**
 * [FORVENTET_INNTEKT] - Ytelse avkortet etter det som var brukeroppgitt forventet årsinntekt i perioden
 *
 * [AARSOPPGJOER] - Iverksatte perioder. Inneholder restanse hvis forventet inntekt endrer seg i løpet av året
 *
 * [ETTEROPPJOER] - TODO ikke enda implementert
 */
enum class AvkortetYtelseType { FORVENTET_INNTEKT, AARSOPPGJOER, ETTEROPPJOER }

fun Beregning.mapTilYtelseFoerAvkorting() =
    beregningsperioder.map {
        YtelseFoerAvkorting(
            beregning = it.utbetaltBeloep,
            periode = Periode(it.datoFOM, it.datoTOM),
            beregningsreferanse = this.beregningId,
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

    val eksisterendeFremTilNye =
        this
            .filter { it.periode.fom < fraOgMedNyYtelse }
            .filter { beregning.beregningId != it.beregningsreferanse }

    val eksisterendeAvrundetPerioder =
        eksisterendeFremTilNye.map { ytelseFoerAvkorting ->
            if (ytelseFoerAvkorting.periode.tom == null ||
                fraOgMedNyYtelse <= ytelseFoerAvkorting.periode.tom
            ) {
                ytelseFoerAvkorting.copy(
                    periode =
                        Periode(
                            fom = ytelseFoerAvkorting.periode.fom,
                            tom = fraOgMedNyYtelse.minusMonths(1),
                        ),
                )
            } else {
                ytelseFoerAvkorting
            }
        }

    return eksisterendeAvrundetPerioder + nyYtelseFoerAvkorting
}
