package no.nav.etterlatte.avkorting

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.avkorting.AvkortetYtelseType.AARSOPPGJOER
import no.nav.etterlatte.avkorting.AvkortetYtelseType.ETTEROPPJOER
import no.nav.etterlatte.avkorting.AvkortetYtelseType.FORVENTET_INNTEKT
import no.nav.etterlatte.beregning.Beregning
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
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
    ) = if (behandlingstype == BehandlingType.FØRSTEGANGSBEHANDLING) {
        oppdaterMedInntektsgrunnlag(nyttGrunnlag, virkningstidspunkt, bruker).beregnAvkortingForstegangs(beregning)
    } else {
        oppdaterMedInntektsgrunnlag(nyttGrunnlag, virkningstidspunkt, bruker).beregnAvkortingRevurdering(beregning)
    }

    fun oppdaterMedInntektsgrunnlag(
        nyttGrunnlag: AvkortingGrunnlagLagreDto,
        virkningstidspunkt: YearMonth,
        bruker: BrukerTokenInfo,
    ): Avkorting {
        // TODO kreve at det er inneværende år?
        val aarsoppgjoer = hentAarsoppgjoer(virkningstidspunkt)
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
                                relevanteMaanederInnAar = aarsoppgjoer.utledRelevanteMaaneder(virkningstidspunkt),
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

    private fun beregnAvkortingForstegangs(beregning: Beregning): Avkorting {
        val aarsoppgjoer = aarsoppgjoer.single()

        val ytelseFoerAvkorting = beregning.mapTilYtelseFoerAvkorting()
        val grunnlag = aarsoppgjoer.inntektsavkorting.first().grunnlag

        val avkortingsperioder =
            AvkortingRegelkjoring.beregnInntektsavkorting(
                periode = grunnlag.periode,
                avkortingGrunnlag = listOf(grunnlag),
            )

        val beregnetAvkortetYtelse =
            AvkortingRegelkjoring.beregnAvkortetYtelse(
                periode = grunnlag.periode,
                ytelseFoerAvkorting = ytelseFoerAvkorting,
                avkortingsperioder = avkortingsperioder,
                type = FORVENTET_INNTEKT,
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
                avkortetYtelseAar =
                    beregnetAvkortetYtelse.map { avkortetYtelse ->
                        avkortetYtelse.copy(
                            id = UUID.randomUUID(),
                            type = AARSOPPGJOER,
                        )
                    },
            )
        return this.copy(
            aarsoppgjoer = erstattAarsoppgjoer(oppdatertAarsoppgjoer),
        )
    }

    fun beregnAvkortingRevurdering(beregning: Beregning): Avkorting {
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
                                fom = aarsoppgjoer.foersteMaanedDetteAar(),
                                tom = inntektsavkorting.grunnlag.periode.tom,
                            )

                        val avkortinger =
                            AvkortingRegelkjoring.beregnInntektsavkorting(
                                periode = periode,
                                avkortingGrunnlag = listOf(inntektsavkorting.grunnlag.copy(periode = periode)),
                            )

                        val avkortetYtelseForventetInntekt =
                            if (aarsoppgjoer.inntektsavkorting.size > 1) {
                                AvkortingRegelkjoring.beregnAvkortetYtelse(
                                    periode = periode,
                                    ytelseFoerAvkorting = ytelseFoerAvkorting,
                                    avkortingsperioder = avkortinger,
                                    type = FORVENTET_INNTEKT,
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
                        beregnAvkortetYtelseMedRestanse(aarsoppgjoer, ytelseFoerAvkorting, reberegnetInntektsavkorting)
                    } else {
                        reberegnetInntektsavkorting.first().let {
                            AvkortingRegelkjoring.beregnAvkortetYtelse(
                                periode = it.grunnlag.periode,
                                ytelseFoerAvkorting = ytelseFoerAvkorting,
                                avkortingsperioder = it.avkortingsperioder,
                                type = AARSOPPGJOER,
                                restanse = null,
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
    ): List<AvkortetYtelse> {
        val avkortetYtelseMedAllForventetInntekt = mutableListOf<AvkortetYtelse>()
        reberegnetInntektsavkorting.forEachIndexed { i, inntektsavkorting ->
            val restanse =
                when (i) {
                    0 -> null
                    else ->
                        AvkortingRegelkjoring.beregnRestanse(
                            aarsoppgjoer.foersteMaanedDetteAar(),
                            inntektsavkorting,
                            avkortetYtelseMedAllForventetInntekt,
                        )
                }

            val ytelse =
                AvkortingRegelkjoring.beregnAvkortetYtelse(
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

    fun hentAarsoppgjoer(fom: YearMonth): Aarsoppgjoer {
        val funnet = aarsoppgjoer.find { it.aar == fom.year }
        return funnet ?: Aarsoppgjoer(UUID.randomUUID(), aar = fom.year)
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
    val relevanteMaanederInnAar: Int,
    val inntektUtland: Int,
    val fratrekkInnAarUtland: Int,
    val spesifikasjon: String,
    val kilde: Grunnlagsopplysning.Saksbehandler,
)

data class Aarsoppgjoer(
    val id: UUID,
    val aar: Int,
    val ytelseFoerAvkorting: List<YtelseFoerAvkorting> = emptyList(),
    val inntektsavkorting: List<Inntektsavkorting> = emptyList(),
    val avkortetYtelseAar: List<AvkortetYtelse> = emptyList(),
) {
	/*
	 * Relevante månder (innvilga måneder i gjeldende år) viderføres i alle grunnlagsperioder. så når man skal
	 * utlede det til ny inntektsperiode kan man ta fra hvilken som helst periode i gjeldende år.
	 * Hvis det ikke finnes noen inntekt for gjeldende år enda (førstegangsbehandling) regnes den ut basert på
	 *  ny virk/innvilgelsesdato.
	 */
    fun utledRelevanteMaaneder(virkningstidspunkt: YearMonth): Int {
        val aaretsFoersteForventaInntekt =
            inntektsavkorting
                .sortedBy { it.grunnlag.periode.fom }
                .firstOrNull {
                    it.grunnlag.periode.fom.year == virkningstidspunkt.year
                }?.grunnlag
        return aaretsFoersteForventaInntekt?.relevanteMaanederInnAar ?: (12 - virkningstidspunkt.monthValue + 1)
    }

	/*
	 * Det er tilfeller hvor det er nødvendig å vite når første periode i inneværende begynner.
	 * Ved inngangsår så vil ikke første måned nødvendgivis være januar så det må baseres på fom første periode.
	 */
    fun foersteMaanedDetteAar(): YearMonth =
        if (ytelseFoerAvkorting.isEmpty()) {
            YearMonth.of(aar, 1)
        } else {
            ytelseFoerAvkorting.first().periode.fom
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
        filter { it.periode.fom < fraOgMedNyYtelse }
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
