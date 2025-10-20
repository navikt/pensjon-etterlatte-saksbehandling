package no.nav.etterlatte.avkorting

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.avkorting.AvkortetYtelseType.AARSOPPGJOER
import no.nav.etterlatte.avkorting.AvkortetYtelseType.ETTEROPPGJOER
import no.nav.etterlatte.avkorting.AvkortetYtelseType.FORVENTET_INNTEKT
import no.nav.etterlatte.avkorting.AvkortingRegelkjoring.beregnInntektInnvilgetPeriodeFaktiskInntekt
import no.nav.etterlatte.avkorting.AvkortingRegelkjoring.beregnInntektInnvilgetPeriodeForventetInntekt
import no.nav.etterlatte.beregning.Beregning
import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FRA
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.beregning.Sanksjon
import no.nav.etterlatte.libs.common.beregning.SanksjonertYtelse
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Innholder alt som har med inntektsavkorting for Omstillingstønad. Både data og domenelogikk.
 * Se videre dokumentasjon i [Aarsoppgjoer], [AvkortingGrunnlag] og nedover.
 */
data class Avkorting(
    val aarsoppgjoer: List<Aarsoppgjoer> = emptyList(),
) {
    init {
        if (!aarsoppgjoer.zipWithNext().all { it.first.aar < it.second.aar }) {
            throw InternfeilException("Årsoppgjør er i feil rekkefølge.")
        }
    }

    /**
     * Å skille på år er kun relevant internt for avkorting. Alle perioder på tvers av alle [Aarsoppgjoer] blir
     * derfor flatet ut til sammenhengende perioder.
     *
     * For [AvkortingDto.avkortingGrunnlag] så er det ønskelig med full historikk.
     * For [AvkortingDto.avkortetYtelse] er det kun ønskelig å hente fra og med virkningstidspunkt til behandling.
     */
    fun toDto(fraVirkningstidspunkt: YearMonth? = null): AvkortingDto {
        val avkortingGrunnlag =
            aarsoppgjoer.flatMap { aarsoppgjoer ->
                when (aarsoppgjoer) {
                    is AarsoppgjoerLoepende ->
                        aarsoppgjoer.inntektsavkorting.map {
                            it.grunnlag.toDto()
                        }

                    is Etteroppgjoer -> {
                        listOf(aarsoppgjoer.inntekt.toDto())
                    }
                }
            }

        val avkortetYtelse =
            if (fraVirkningstidspunkt !=
                null
            ) {
                aarsoppgjoer.filter { fraVirkningstidspunkt.year <= it.aar }.flatMap { aarsoppgjoer ->
                    aarsoppgjoer.avkortetYtelse
                        .filter { it.periode.tom == null || fraVirkningstidspunkt <= it.periode.tom }
                        .map {
                            if (fraVirkningstidspunkt > it.periode.fom &&
                                (it.periode.tom == null || fraVirkningstidspunkt <= it.periode.tom)
                            ) {
                                it.copy(periode = Periode(fom = fraVirkningstidspunkt, tom = it.periode.tom))
                            } else {
                                it
                            }
                        }.map {
                            it.toDto()
                        }
                }
            } else {
                aarsoppgjoer.flatMap { aarsoppgjoer ->
                    aarsoppgjoer.avkortetYtelse.map { it.toDto() }
                }
            }

        return AvkortingDto(
            avkortingGrunnlag = avkortingGrunnlag,
            avkortetYtelse = avkortetYtelse,
        )
    }

    /**
     * Skal kun benyttes ved opprettelse av ny avkorting ved revurdering.
     */
    fun kopierAvkorting(opphoerFom: YearMonth? = null): Avkorting =
        Avkorting(
            aarsoppgjoer =
                aarsoppgjoer.map {
                    when (it) {
                        is AarsoppgjoerLoepende ->
                            it.copy(
                                id = UUID.randomUUID(),
                                inntektsavkorting =
                                    it.inntektsavkorting.map { inntektsavkorting ->
                                        val tom =
                                            inntektsavkorting.grunnlag.periode.tom
                                                ?: opphoerFom?.let {
                                                    if (opphoerFom.year > inntektsavkorting.grunnlag.periode.fom.year) {
                                                        YearMonth.of(
                                                            inntektsavkorting.grunnlag.periode.fom.year,
                                                            Month.DECEMBER,
                                                        )
                                                    } else {
                                                        opphoerFom.minusMonths(1)
                                                    }
                                                }
                                        inntektsavkorting.copy(
                                            grunnlag =
                                                inntektsavkorting.grunnlag.copy(
                                                    id = UUID.randomUUID(),
                                                    periode =
                                                        inntektsavkorting.grunnlag.periode.copy(
                                                            fom = inntektsavkorting.grunnlag.periode.fom,
                                                            tom = tom,
                                                        ),
                                                ),
                                        )
                                    },
                            )

                        is Etteroppgjoer ->
                            it.copy(
                                id = UUID.randomUUID(),
                                inntekt =
                                    it.inntekt.copy(
                                        id = UUID.randomUUID(),
                                    ),
                            )
                    }
                },
        )

    /**
     * Brukes til å beregne avkoring med flere forventede inntekter [ForventetInntekt]
     */
    fun beregnAvkortingMedNyeGrunnlag(
        nyttGrunnlag: List<AvkortingGrunnlagLagreDto>,
        bruker: BrukerTokenInfo,
        beregning: Beregning,
        sanksjoner: List<Sanksjon>,
        opphoerFom: YearMonth?,
        aldersovergang: YearMonth? = null,
    ): Avkorting {
        var oppdatertAvkorting = this
        nyttGrunnlag.forEach {
            oppdatertAvkorting = oppdatertAvkorting.oppdaterMedInntektsgrunnlag(it, bruker, opphoerFom, aldersovergang)
        }

        return oppdatertAvkorting.beregnAvkorting(
            beregning.beregningsperioder.minOf { it.datoFOM },
            beregning,
            sanksjoner,
        )
    }

    /**
     * Legger til brukeroppgitt [ForventetInntekt] i [AarsoppgjoerLoepende].
     */
    fun oppdaterMedInntektsgrunnlag(
        nyttGrunnlag: AvkortingGrunnlagLagreDto,
        bruker: BrukerTokenInfo,
        opphoerFom: YearMonth? = null,
        aldersovergang: YearMonth? = null,
    ): Avkorting {
        val aarsoppgjoer =
            hentEllerOpprettAarsoppgjoer(nyttGrunnlag.fom) as? AarsoppgjoerLoepende
                ?: throw InternfeilException("Kan ikke oppdatere inntektsgrunnlag for et år som har etteroppgjør")

        val tom = opphoerFom?.let { finnTomForInntekt(opphoerFom, aarsoppgjoer.aar) }

        val inntektTom = nyttGrunnlag.inntektTom
        val fratrekkInnAar = nyttGrunnlag.fratrekkInnAar
        val inntektUtlandTom = nyttGrunnlag.inntektUtlandTom
        val fratrekkInnAarUtland = nyttGrunnlag.fratrekkInnAarUtland
        val kilde = Grunnlagsopplysning.Saksbehandler(bruker.ident(), Tidspunkt.now())
        val periode = Periode(fom = nyttGrunnlag.fom, tom = tom)

        // Ved revurdering tilbake i tid - betyr det at fom i årsoppgjøret også må flyttes til fom i nytt inntektsgrunnlag
        val gjeldendeAaarsoppgjoerFom = if (nyttGrunnlag.fom < aarsoppgjoer.fom) nyttGrunnlag.fom else aarsoppgjoer.fom

        val forventetInntekt =
            ForventetInntekt(
                id = nyttGrunnlag.id,
                periode = periode,
                inntektTom = inntektTom,
                fratrekkInnAar = fratrekkInnAar,
                inntektUtlandTom = inntektUtlandTom,
                fratrekkInnAarUtland = fratrekkInnAarUtland,
                innvilgaMaaneder =
                    nyttGrunnlag.overstyrtInnvilgaMaaneder?.antall
                        ?: finnAntallInnvilgaMaanederForAar(gjeldendeAaarsoppgjoerFom, tom, aldersovergang),
                overstyrtInnvilgaMaanederAarsak =
                    nyttGrunnlag.overstyrtInnvilgaMaaneder?.aarsak?.let {
                        OverstyrtInnvilgaMaanederAarsak.valueOf(it)
                    } ?: aldersovergang?.let { OverstyrtInnvilgaMaanederAarsak.BLIR_67 },
                overstyrtInnvilgaMaanederBegrunnelse =
                    nyttGrunnlag.overstyrtInnvilgaMaaneder?.begrunnelse
                        ?: aldersovergang?.let { "Bruker har aldersovergang" },
                spesifikasjon = nyttGrunnlag.spesifikasjon,
                kilde = kilde,
                inntektInnvilgetPeriode =
                    beregnInntektInnvilgetPeriodeForventetInntekt(
                        inntektTom,
                        fratrekkInnAar,
                        inntektUtlandTom,
                        fratrekkInnAarUtland,
                        kilde,
                        periode,
                    ),
            )

        val oppdatert =
            aarsoppgjoer.inntektsavkorting
                // Kun ta med perioder før nytt virkningstidspunkt - revurdering bakover i tid vil fjerne alt etter
                // Dette vil også gjelde ved redigering
                .filter { it.grunnlag.periode.fom < nyttGrunnlag.fom }
                .map { it.lukkSisteInntektsperiode(nyttGrunnlag.fom, tom) }
                .plus(Inntektsavkorting(grunnlag = forventetInntekt))

        val oppdatertAarsoppjoer =
            aarsoppgjoer.copy(
                inntektsavkorting = oppdatert,
                fom = gjeldendeAaarsoppgjoerFom,
            )
        return erstattAarsoppgjoer(oppdatertAarsoppjoer)
    }

    /**
     * Hvis det finnes opphør i inntektsår skal måned før opphør fra og med være til og med til [ForventetInntekt].
     * Ellers skal til og med være desember.
     */
    private fun finnTomForInntekt(
        opphoerFom: YearMonth,
        inntektsaar: Int,
    ): YearMonth =
        if (opphoerFom.year == inntektsaar) {
            opphoerFom.minusMonths(1)
        } else {
            YearMonth.of(inntektsaar, Month.DECEMBER)
        }

    /**
     * Brukes til å ta i bruk [FaktiskInntekt] til et [Etteroppgjoer].
     */
    fun beregnEtteroppgjoer(
        brukerTokenInfo: BrukerTokenInfo,
        aar: Int,
        loennsinntekt: Int,
        afp: Int,
        naeringsinntekt: Int,
        utland: Int,
        sanksjoner: List<Sanksjon>,
        spesifikasjon: String,
    ): Avkorting {
        val tidligereAarsoppgjoer = aarsoppgjoer.single { aarsoppgjoer -> aarsoppgjoer.aar == aar }

        // TODO kan ha endret seg? slik at forrige behandling sin avkorting ikke lenger stemmer?
        val innvilgetPeriodeIAaret = tidligereAarsoppgjoer.periode()

        val kilde = Grunnlagsopplysning.Saksbehandler(brukerTokenInfo.ident(), Tidspunkt.now())
        val inntekt =
            FaktiskInntekt(
                id = UUID.randomUUID(),
                periode = innvilgetPeriodeIAaret,
                innvilgaMaaneder = tidligereAarsoppgjoer.innvilgaMaaneder(),
                loennsinntekt = loennsinntekt,
                naeringsinntekt = naeringsinntekt,
                utlandsinntekt = utland,
                afp = afp,
                kilde = kilde,
                spesifikasjon = spesifikasjon,
                inntektInnvilgetPeriode =
                    beregnInntektInnvilgetPeriodeFaktiskInntekt(
                        loennsinntekt = loennsinntekt,
                        afp = afp,
                        naeringsinntekt = naeringsinntekt,
                        utland = utland,
                        periode =
                            RegelPeriode(
                                fraDato = LocalDate.of(aar, Month.JANUARY.value, 1),
                                tilDato = LocalDate.of(aar, Month.DECEMBER, 31),
                            ),
                        kilde = kilde,
                    ),
            )

        val etteroppgjoer =
            Etteroppgjoer(
                id = UUID.randomUUID(),
                aar = tidligereAarsoppgjoer.aar,
                fom = innvilgetPeriodeIAaret.fom,
                ytelseFoerAvkorting = tidligereAarsoppgjoer.ytelseFoerAvkorting,
                inntekt = inntekt,
            )
        val nyAvkorting = erstattAarsoppgjoer(etteroppgjoer)
        return nyAvkorting.beregnAvkorting(tidligereAarsoppgjoer.fom, null, sanksjoner)
    }

    /**
     * Beregner all avkorting for hele [Avkorting]. Det vil si alle år som har hatt utbetaling eller
     * inneværende år ([Aarsoppgjoer]).
     *
     * Avkorting beregnes ulikt for [AarsoppgjoerLoepende] og [Etteroppgjoer].
     * Se [beregnAvkortingLoepende] og [beregnAvkortingEtteroppgjoer]
     *
     * @param beregning - Forbehandling av [Etteroppgjoer] har ikke [Beregning]. Vil derfor videreføre ekisterende
     * [Aarsoppgjoer.ytelseFoerAvkorting].
     */
    fun beregnAvkorting(
        virkningstidspunkt: YearMonth,
        beregning: Beregning?,
        sanksjoner: List<Sanksjon>,
    ): Avkorting {
        val virkningstidspunktAar = virkningstidspunkt.year

        val oppdaterteOppgjoer =
            (aarsoppgjoer).map { aarsoppgjoer ->

                val ytelseFoerAvkorting =
                    if (beregning != null && aarsoppgjoer.aar >= virkningstidspunktAar) {
                        aarsoppgjoer.ytelseFoerAvkorting.leggTilNyeBeregninger(beregning)
                    } else {
                        aarsoppgjoer.ytelseFoerAvkorting
                    }

                when (aarsoppgjoer) {
                    is AarsoppgjoerLoepende -> beregnAvkortingLoepende(aarsoppgjoer, ytelseFoerAvkorting, sanksjoner)
                    is Etteroppgjoer -> beregnAvkortingEtteroppgjoer(aarsoppgjoer, ytelseFoerAvkorting, sanksjoner)
                }
            }

        return copy(aarsoppgjoer = oppdaterteOppgjoer)
    }

    /**
     * Beregner avkorting for et år med en eller flere brukeroppgitte [ForventetInntekt] med følgende steg.
     * 1. Beregner/reberegner [Inntektsavkorting] for alle [AarsoppgjoerLoepende.inntektsavkorting]er.
     * 2. Beregner [AarsoppgjoerLoepende.avkortetYtelse] med restanse om det finnes
     * flere [AarsoppgjoerLoepende.inntektsavkorting] (Se [beregnAvkortetYtelseMedRestanse]).
     */
    private fun beregnAvkortingLoepende(
        aarsoppgjoer: AarsoppgjoerLoepende,
        ytelseFoerAvkorting: List<YtelseFoerAvkorting>,
        sanksjoner: List<Sanksjon>,
    ): AarsoppgjoerLoepende {
        val reberegnetInntektsavkorting =
            aarsoppgjoer.inntektsavkorting.map { inntektsavkorting ->
                val periode =
                    Periode(
                        fom = aarsoppgjoer.fom,
                        tom = inntektsavkorting.grunnlag.periode.tom,
                    )

                val inntekt =
                    when (inntektsavkorting.grunnlag.inntektInnvilgetPeriode) {
                        is BenyttetInntektInnvilgetPeriode -> inntektsavkorting.grunnlag
                        is IngenInntektInnvilgetPeriode ->
                            inntektsavkorting.grunnlag.copy(
                                inntektInnvilgetPeriode =
                                    with(inntektsavkorting.grunnlag) {
                                        beregnInntektInnvilgetPeriodeForventetInntekt(
                                            inntektTom,
                                            fratrekkInnAar,
                                            inntektUtlandTom,
                                            fratrekkInnAarUtland,
                                            kilde,
                                            periode,
                                        )
                                    },
                            )
                    }
                val avkortinger =
                    AvkortingRegelkjoring.beregnInntektsavkorting(
                        periode = periode,
                        avkortingGrunnlag = inntekt,
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
                    val tomDesember = tomDesemberForAvkortetYtelse(aarsoppgjoer)
                    val sistePeriode =
                        when (it.grunnlag.periode.tom) {
                            null -> Periode(fom = it.grunnlag.periode.fom, tom = tomDesember)
                            else -> it.grunnlag.periode
                        }
                    AvkortingRegelkjoring.beregnAvkortetYtelse(
                        periode = sistePeriode,
                        ytelseFoerAvkorting = ytelseFoerAvkorting,
                        avkortingsperioder = it.avkortingsperioder,
                        type = AARSOPPGJOER,
                        restanse = null,
                        sanksjoner = sanksjoner,
                    )
                }
            }

        return aarsoppgjoer.copy(
            ytelseFoerAvkorting = ytelseFoerAvkorting,
            inntektsavkorting = reberegnetInntektsavkorting,
            avkortetYtelse = avkortetYtelse,
        )
    }

    /**
     * Beregner [AvkortetYtelse] for hver [Inntektsavkorting.grunnlag] periodsert basert på [ForventetInntekt.periode].
     *
     * Finnes det flere [Inntektsavkorting] beregnes det også en [Restanse] som legges til beregningen av [AvkortetYtelse].
     *
     * [Restanse] beregnes ved å sammenligne hva [AvkortetYtelse] ville vært med
     * [Inntektsavkorting.grunnlag] ([Inntektsavkorting.avkortetYtelseForventetInntekt]) med tidigere perioder med [AvkortetYtelse]
     *
     * @param reberegnetInntektsavkorting vil inneholde hva avkorting ville vært for hver [ForventetInntekt]
     */
    private fun beregnAvkortetYtelseMedRestanse(
        aarsoppgjoer: AarsoppgjoerLoepende,
        ytelseFoerAvkorting: List<YtelseFoerAvkorting>,
        reberegnetInntektsavkorting: List<Inntektsavkorting>,
        sanksjoner: List<Sanksjon>,
    ): List<AvkortetYtelse> {
        val sorterteSanksjonerInnenforAarsoppgjoer =
            aarsoppgjoer
                .sanksjonerInnenforAarsoppjoer(sanksjoner)
                .sortedBy { it.fom }
        val avkortetYtelseMedAllForventetInntekt = mutableListOf<AvkortetYtelse>()

        val tomDesember = tomDesemberForAvkortetYtelse(aarsoppgjoer)

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
                            aarsoppgjoer.fom,
                            inntektsavkorting,
                            avkortetYtelseMedAllForventetInntekt,
                            kjenteSanksjonerForInntektsavkorting,
                        )
                }

            val erSistePeriodeUtenOpphoer =
                reberegnetInntektsavkorting
                    .maxBy {
                        it.grunnlag.periode.fom
                    }.grunnlag.periode
                    .let {
                        it.fom == inntektsavkorting.grunnlag.periode.fom && it.tom == null
                    }

            val ytelse =
                AvkortingRegelkjoring.beregnAvkortetYtelse(
                    periode =
                        when (erSistePeriodeUtenOpphoer) {
                            true -> Periode(inntektsavkorting.grunnlag.periode.fom, tomDesember)
                            false -> inntektsavkorting.grunnlag.periode
                        },
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
                    aarsoppgjoer.fom,
                    reberegnetInntektsavkorting.last(),
                    avkortetYtelseMedAllForventetInntekt,
                    sorterteSanksjonerInnenforAarsoppgjoer,
                )
            // Ytelse etter avkorting må reberegnes fra første sanksjon som ikke er "sett" i tidlegere beregninger
            val tidligsteFomIkkeBeregnetSanksjon =
                krevIkkeNull(sorterteSanksjonerInnenforAarsoppgjoer.firstOrNull { it.fom >= senesteInntektsjusteringFom }?.fom) {
                    "Fant tidligere at vi har en sanksjon som er etter siste inntektsjustering, men finner ingen nå"
                }
            val ytelseMedAlleSanksjoner =
                AvkortingRegelkjoring.beregnAvkortetYtelse(
                    periode = Periode(fom = tidligsteFomIkkeBeregnetSanksjon, tom = tomDesember),
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

    /**
     * Hvis det finnes [Aarsoppgjoer] for et gitt år finnes det også [AvkortetYtelse]/utbetaligner for neste år.
     * Da må siste periode i et år avsluttes.
     * Hvis det ikke finnes [Aarsoppgjoer] neste år må siste periode være åpen for å tilfredstille Oppdrag.
     */
    private fun tomDesemberForAvkortetYtelse(aarsoppgjoer: AarsoppgjoerLoepende): YearMonth? {
        val harAaroppgjoerNesteAaar =
            this.aarsoppgjoer.any { it.aar == aarsoppgjoer.aar + 1 }
        return when (harAaroppgjoerNesteAaar) {
            true -> YearMonth.of(aarsoppgjoer.aar, 12)
            false -> null
        }
    }

    /**
     * Beregnes [Avkortingsperiode] og [AvkortetYtelse] for et [Etteroppgjoer]
     */
    private fun beregnAvkortingEtteroppgjoer(
        etteroppgjoer: Etteroppgjoer,
        ytelseFoerAvkorting: List<YtelseFoerAvkorting>,
        sanksjoner: List<Sanksjon>,
    ): Etteroppgjoer {
        val avkortinger =
            AvkortingRegelkjoring.beregnInntektsavkorting(
                periode = etteroppgjoer.inntekt.periode,
                avkortingGrunnlag = etteroppgjoer.inntekt,
            )

        val avkortetYtelseFaktiskInntekt =
            AvkortingRegelkjoring.beregnAvkortetYtelse(
                periode = etteroppgjoer.inntekt.periode,
                ytelseFoerAvkorting = ytelseFoerAvkorting,
                avkortingsperioder = avkortinger,
                type = ETTEROPPGJOER,
                restanse = null,
                sanksjoner = sanksjoner,
            )

        return etteroppgjoer.copy(
            ytelseFoerAvkorting = ytelseFoerAvkorting,
            avkortingsperioder = avkortinger,
            avkortetYtelse = avkortetYtelseFaktiskInntekt,
        )
    }

    /**
     * Hvilket [Aarsoppgjoer] som er relevant basers på virkningstidspunkt.
     * Hvis det ikke finnes et fra før på virkningstidspunkt opprettes et nytt.
     *
     * Ved innvilgelse/førstegangsbehandling så skal måneder før virkningsitdspunkt trekkes i fra
     * forventa innvilgede måneder.
     */
    private fun hentEllerOpprettAarsoppgjoer(fom: YearMonth): Aarsoppgjoer {
        val funnet = aarsoppgjoer.find { it.aar == fom.year }
        return funnet ?: AarsoppgjoerLoepende(
            id = UUID.randomUUID(),
            aar = fom.year,
            fom = fom,
        )
    }

    fun erstattAarsoppgjoer(nytt: Aarsoppgjoer): Avkorting {
        if (aarsoppgjoer.any { it.aar == nytt.aar }) {
            return this.copy(aarsoppgjoer = aarsoppgjoer.map { if (it.aar == nytt.aar) nytt else it })
        }
        return this.copy(aarsoppgjoer = (aarsoppgjoer + listOf(nytt)).sortedBy { it.aar })
    }
}

/**
 * Grunnlag for beregning av inntektsavkorting er bruker sin innekt i innvilgede måneder innenfor et år.
 * Kilde til inntekt vil variere basert på om det finnes et skatteoppgjør for gjeldende år.
 * Se [ForventetInntekt] og [FaktiskInntekt]
 *
 * @property periode anvendes ulike mellom [ForventetInntekt] og [FaktiskInntekt]
 * @property innvilgaMaaneder Det beregnes frem til et månedlig avkortingsbeløp og da må inntekt deles på innvilgede måneder
 * @property inntektInnvilgetPeriode Inntekten som benyttes til beregning av avkorting.
 *              Utledes ulikt for [ForventetInntekt] og [FaktiskInntekt]
 * @property kilde Hvilken saksbehandler som har fylt inn inntektsopplysnigner.
 */
sealed class AvkortingGrunnlag {
    abstract val id: UUID
    abstract val periode: Periode
    abstract val innvilgaMaaneder: Int
    abstract val inntektInnvilgetPeriode: InntektInnvilgetPeriode
    abstract val spesifikasjon: String
    abstract val kilde: Grunnlagsopplysning.Saksbehandler
}

/**
 * Før skatteoppgjør er bruker nødt til å oppgi hva de forventer å tjene.
 * Siden det oppgis i forkant/løpende i et inntektsår kan det også endre seg.
 *
 * @property periode - fra og med måned etter bruker har oppgitt forventet inntekt til og med opphør eller desember.
 * Eller til bruker oppgir ny inntekt.
 * NB! I tidligere behandlinger er til og med for nyligiste inntekt null. Det har ingen praktisk betydning så det er ikke patchet.
 *
 * @property inntektTom Alle relevante inntekter for oms i Norge fra og med januer til desember eller til opphør.
 * @property fratrekkInnAar Alle relevante inntekter fra januar til innvilget OMS
 * @property inntektUtlandTom Samme som [inntektTom] men for utland
 * @property fratrekkInnAarUtland Samme som [fratrekkInnAarUtland] men for utland
 *
 * @property innvilgaMaaneder se [AvkortingGrunnlag].
 * @property overstyrtInnvilgaMaanederAarsak - Antall utledes automatisk men kan overstyres manuelt om systemet ikke
 * fanger opp et opphør.
 *
 * @property inntektInnvilgetPeriode Inntekten som benyttes til videre beregning av avkorting.
 * inntektInnvilgetPeriode beregnes av [inntektTom], [fratrekkInnAar], [inntektUtlandTom] og [fratrekkInnAarUtland].
 * Se [AvkortingRegelkjoring.beregnInntektInnvilgetPeriodeForventetInntekt].
 */
data class ForventetInntekt(
    override val id: UUID,
    override val periode: Periode,
    val inntektTom: Int,
    val fratrekkInnAar: Int,
    val inntektUtlandTom: Int,
    val fratrekkInnAarUtland: Int,
    override val innvilgaMaaneder: Int,
    override val spesifikasjon: String,
    override val kilde: Grunnlagsopplysning.Saksbehandler,
    val overstyrtInnvilgaMaanederAarsak: OverstyrtInnvilgaMaanederAarsak? = null,
    val overstyrtInnvilgaMaanederBegrunnelse: String? = null,
    override val inntektInnvilgetPeriode: InntektInnvilgetPeriode,
) : AvkortingGrunnlag()

/**
 * Etter skatteoppgjør vil det gjennomføres et etteroppgjør hvor faktisk inntekt hentes inn.
 *
 * @property periode - Innvilget periode for gjeldende år
 *
 * @property innvilgaMaaneder se [AvkortingGrunnlag].
 *
 * @property inntektInnvilgetPeriode Inntekten som benyttes til videre beregning av avkorting.
 * inntektInnvilgetPeriode beregnes av [loennsinntekt], [naeringsinntekt], [afp] og [utlandsinntekt].
 * Se [AvkortingRegelkjoring.beregnInntektInnvilgetPeriodeFaktiskInntekt].
 */

data class FaktiskInntekt(
    override val id: UUID,
    override val periode: Periode,
    override val innvilgaMaaneder: Int,
    val loennsinntekt: Int,
    val naeringsinntekt: Int,
    val afp: Int,
    val utlandsinntekt: Int,
    override val spesifikasjon: String,
    override val kilde: Grunnlagsopplysning.Saksbehandler,
    override val inntektInnvilgetPeriode: BenyttetInntektInnvilgetPeriode,
) : AvkortingGrunnlag()

/**
 * Dette beløpet brukes videre i beregning av avkorting.
 * Det utledes ulikt for [ForventetInntekt] og [FaktiskInntekt].
 * Se [AvkortingRegelkjoring.beregnInntektInnvilgetPeriodeFaktiskInntekt] og
 * [AvkortingRegelkjoring.beregnInntektInnvilgetPeriodeForventetInntekt]
 */
sealed class InntektInnvilgetPeriode

data class BenyttetInntektInnvilgetPeriode(
    val verdi: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode,
    val kilde: Grunnlagsopplysning.RegelKilde,
) : InntektInnvilgetPeriode()

/**
 * [InntektInnvilgetPeriode] som et objekt som produseres med regelkode var ikke i bruk fra oppstart av OMS.
 * Det finnes derfor behandlinger som ikke har denne.
 * I disse tilfellene benyttes [IngenInntektInnvilgetPeriode].
 * Se historikk til regelkode for avkorting for mer info ([AvkortingRegelkjoring.beregnInntektsavkorting]).
 */
data object IngenInntektInnvilgetPeriode : InntektInnvilgetPeriode()

enum class OverstyrtInnvilgaMaanederAarsak {
    TAR_UT_PENSJON_TIDLIG,
    BLIR_67,
    ANNEN,
}

/**
 * Skal inneholde alt som har med avkorting å gjøre innenfor innvelgede månder for et inntektsår.
 * Kan være et år hvor avkorting har blitt gjennomført med forventet inntekt oppgitt av bruker ([AarsoppgjoerLoepende])
 * eller en faktisk inntekt ([Etteroppgjoer])
 *
 * @property aar - Året inntektsavkortingen gjelder
 * @property fom - Første måned i inntektsåret som er innvilget
 * @property ytelseFoerAvkorting er beregning. Anvendes ulikt. Se doc i [AarsoppgjoerLoepende] og [Etteroppgjoer]
 * @property avkortetYtelse Anvendes ulikt. Se doc i [AarsoppgjoerLoepende] og [Etteroppgjoer]
 */
sealed class Aarsoppgjoer {
    abstract val id: UUID
    abstract val aar: Int
    abstract val fom: YearMonth
    abstract val ytelseFoerAvkorting: List<YtelseFoerAvkorting>
    abstract val avkortetYtelse: List<AvkortetYtelse>

    fun innvilgaMaaneder() =
        when (this) {
            is AarsoppgjoerLoepende ->
                this.inntektsavkorting
                    .last()
                    .grunnlag.innvilgaMaaneder

            is Etteroppgjoer -> this.inntekt.innvilgaMaaneder
        }

    fun periode() =
        when (this) {
            is AarsoppgjoerLoepende ->
                Periode(
                    fom = fom,
                    tom =
                        inntektsavkorting
                            .last()
                            .grunnlag.periode.tom ?: YearMonth.of(aar, 12),
                )

            is Etteroppgjoer -> this.inntekt.periode
        }
}

/**
 * Inneholder alt som har med avkorting å gjøre innenfor innvilgede månder for et inntektsår som IKKE har hatt skatteoppgjør.
 *
 * Opprettes av [Avkorting.hentEllerOpprettAarsoppgjoer]
 * Beregnes/fylles ut [Avkorting.beregnAvkorting]
 *
 * @property aar - se [Aarsoppgjoer]
 * @property fom - se [Aarsoppgjoer]
 * @property ytelseFoerAvkorting er beregning. Vi er nødt til å lagre beregning for hele året (eller innvilget periode) for å
 * beregne restanse. Beregning ellers lagres bare fra og med virkningstidspunkt til behandling.
 * @property inntektsavkorting - Inneholder det avkorting ville vært for hele år (eller innvilget periode) for hver enkelt brukeroppgitt
 * forvetet inntekt.
 * @property avkortetYtelse er sammensatt av [Inntektsavkorting.avkortetYtelseForventetInntekt] basert på
 * når ny forventet inntekt ble oppgitt.
 */
data class AarsoppgjoerLoepende(
    override val id: UUID,
    override val aar: Int,
    override val fom: YearMonth,
    override val ytelseFoerAvkorting: List<YtelseFoerAvkorting> = emptyList(),
    val inntektsavkorting: List<Inntektsavkorting> = emptyList(),
    override val avkortetYtelse: List<AvkortetYtelse> = emptyList(),
) : Aarsoppgjoer() {
    init {
        fun periodeISammeAar(periode: Periode) {
            if (periode.fom.year != aar || (periode.tom?.year?.let { it != aar } == true)) {
                throw InternfeilException("Perioder må være innenfor årsoppgjøret sitt år $periode")
            }
        }

        with(ytelseFoerAvkorting) {
            if (!zipWithNext().all { it.first.periode.fom < it.second.periode.fom }) {
                throw InternfeilException("fom for YtelseFoerAvkorting er i feil rekkefølge")
            }
        }

        with(inntektsavkorting) {
            if (!zipWithNext().all { it.first.grunnlag.periode.fom < it.second.grunnlag.periode.fom }) {
                throw InternfeilException("fom for inntektsavkorting er i feil rekkefølge")
            }
            forEach {
                periodeISammeAar(it.grunnlag.periode)
            }
        }

        with(avkortetYtelse) {
            if (!zipWithNext().all { it.first.periode.fom < it.second.periode.fom }) {
                throw InternfeilException("fom for AvkortetYtelseAar er i feil rekkefølge")
            }
            forEach {
                periodeISammeAar(it.periode)
            }
        }
    }

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
            val overlapperTom = sanksjon.fom.year <= aar && (sanksjon.tom == null || sanksjon.tom!!.year >= aar)
            erFomDetteAaret || overlapperTom
        }
}

/**
 * Inneholder alt som har med avkorting å gjøre innenfor innvilgede månder for et inntektsår som har hatt skatteoppgjør.
 *
 * Denne beregnes/fylles ut i to sammenhenter. Først under en forbehandling deretter under en revurdering.
 * Opprettes og beregnes/fylles ut [Avkorting.beregnEtteroppgjoer]
 *
 * @property aar - se [Aarsoppgjoer]
 * @property fom - se [Aarsoppgjoer]
 * @property ytelseFoerAvkorting er beregning. Forbehandling av etteroppgjør har ikke beregning og er avhengig
 * av å videreføre dette feltet.
 * @property inntekt - Etter et skatteoppgjør vet vi hva inntekt faktisk er og kan bruke dene ene for hele året.
 * Derav et felt for inntekt i motsetning til liste som i [AarsoppgjoerLoepende].
 * @property avkortingsperioder beregnede avkortinger med en [FaktiskInntekt].
 * @property avkortetYtelse beregning etter avkorting med [FaktiskInntekt].
 */
data class Etteroppgjoer(
    override val id: UUID,
    override val aar: Int,
    override val fom: YearMonth,
    override val ytelseFoerAvkorting: List<YtelseFoerAvkorting> = emptyList(),
    val inntekt: FaktiskInntekt,
    val avkortingsperioder: List<Avkortingsperiode> = emptyList(),
    override val avkortetYtelse: List<AvkortetYtelse> = emptyList(),
) : Aarsoppgjoer()

/**
 * Inneholder en [ForventetInntekt] med en periode inntekten var gjeldende (se [ForventetInntekt.periode]).
 *
 * I tillegg inneholder den beregnet avkorting med [Inntektsavkorting.grunnlag] for alle innvilgede måneder i gjeldene år for en.
 *
 * Det er kun [Inntektsavkorting.avkortingsperioder] innenfor [Inntektsavkorting.grunnlag] sin periode som brukes
 * beregning av [Aarsoppgjoer.avkortetYtelse] som fører til utbetaling.
 * Resten brukes IKKE for å avgjøre hva som skal utbetales men
 * som grunnlag til å beregne [Restanse] ([Avkorting.beregnAvkortetYtelseMedRestanse]).
 *
 * @property grunnlag - Brukeroppgit [ForventetInntekt]
 * @property avkortingsperioder - utregnet basert på [ForventetInntekt].
 * @property avkortetYtelseForventetInntekt - Beregning etter avkorting for en [ForventetInntekt].
 * Brukes til å beregne restanse IKKE hva som utbetales. Utbetaling avgjøres av [AarsoppgjoerLoepende.avkortetYtelse].
 */
data class Inntektsavkorting(
    val grunnlag: ForventetInntekt,
    val avkortingsperioder: List<Avkortingsperiode> = emptyList(),
    val avkortetYtelseForventetInntekt: List<AvkortetYtelse> = emptyList(),
) {
    init {
        if (!avkortingsperioder.zipWithNext().all { it.first.periode.fom < it.second.periode.fom }) {
            throw InternfeilException("Avkortingsperioder er i feil rekkefølge.")
        }
        if (!avkortetYtelseForventetInntekt.zipWithNext().all { it.first.periode.fom < it.second.periode.fom }) {
            throw InternfeilException("AvkortetYtelseForventetInntekt er i feil rekkefølge.")
        }
    }

    fun lukkSisteInntektsperiode(
        virkningstidspunkt: YearMonth,
        tom: YearMonth?,
    ) = if (grunnlag.periode.tom == null || grunnlag.periode.tom == tom) {
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
    } else {
        this
    }
}

/**
 * Beregnet ytelse (ytelse før avkorting / [Beregning]) persisteres for hele år for å kunne
 * beregne ytelse etter avkorting for et helt år av gangen. Det er nødvendig for [Restanse] og [Etteroppgjoer].
 * (se [leggTilNyeBeregninger]).
 *
 * @property beregningsreferanse id til duplisert [Beregning].
 */
data class YtelseFoerAvkorting(
    val beregning: Int,
    val periode: Periode,
    val beregningsreferanse: UUID,
)

/**
 * Utregnet avkortingsbeløp basert på [AvkortingGrunnlag] som benyttes til å avkorte ytelse.
 * @property inntektsgrunnlag id til benyttet [AvkortingGrunnlag].
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
 * For et [AarsoppgjoerLoepende] når en [ForventetInntekt] har endret seg i løpet av året vil det ha blitt avkortet for lite eller for mye.
 * Dette avviket blir "restanse" som fordeles over gjenværende måneder av året for å minimere avvik på etteroppgjøret.
 * (se [Avkorting.beregnAvkortetYtelseMedRestanse]).
 *
 * @property totalRestanse Summen av restansen til alle måneder som har hatt utbetaling med en tidligere [ForventetInntekt]
 * @property fordeltRestanse Beløpet som fordeles på gjenværende måneder. [totalRestanse] delt på antall gjenværende måneder
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
 * Ytelse etter avkortingsbeløp ([Avkortingsperiode]) er blit trukket ifra [YtelseFoerAvkorting] pluss/minsu eventuell [Restanse].
 *
 * @property type [AvkortetYtelse] beregnes ulikt hvis det er [FaktiskInntekt] eller [ForventetInntekt]. Se [AvkortetYtelseType].
 * @property avkortingsbeloep - Videreført beløp fra [Avkortingsperiode]
 * @property ytelseFoerAvkorting - En videreføring av [Aarsoppgjoer.ytelseFoerAvkorting].
 * @property ytelseEtterAvkortingFoerRestanse - Beløp etter beregning er truket ifra med [avkortingsbeloep].
 * @property ytelseEtterAvkorting - Samme som [ytelseEtterAvkortingFoerRestanse] men med [Restanse]
 * @property inntektsgrunnlag - Id til benyttest [AvkortingGrunnlag]
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
 * [FORVENTET_INNTEKT] - OBS! [AvkortetYtelse] med denne typen er IKKE brukt til utbetaling.
 * Disse er kun brukt til å beregne [Restanse]. Se [Avkorting.beregnAvkortetYtelseMedRestanse].
 *
 * [AARSOPPGJOER] - Benyttes i [AarsoppgjoerLoepende.avkortetYtelse]. Brukes til utbetaling.
 *
 * [ETTEROPPGJOER] - Benyttes i [Etteroppgjoer.avkortetYtelse]. Brukes til utbetaling.
 */
enum class AvkortetYtelseType { FORVENTET_INNTEKT, AARSOPPGJOER, ETTEROPPGJOER }

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

fun finnAntallInnvilgaMaanederForAar(
    aarsoppgjoerFom: YearMonth,
    tom: YearMonth?,
    aldersovergang: YearMonth?,
): Int {
    val tomMaaned = tom ?: aldersovergang?.minusMonths(1)
    val tomEllerDesember = tomMaaned?.monthValue ?: 12
    return tomEllerDesember - (aarsoppgjoerFom.monthValue - 1)
}

data class InnvilgedeMaanederGrunnlag(
    val beregningsperioder: FaktumNode<List<YtelseFoerAvkorting>>,
    val aldersovergang: FaktumNode<YearMonth?>,
)

val perioderMedYtelse =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        finnFaktum = InnvilgedeMaanederGrunnlag::beregningsperioder,
        finnFelt = { it },
    )

val aldersovergang =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        finnFaktum = InnvilgedeMaanederGrunnlag::aldersovergang,
        finnFelt = { it },
    )

val perioderMedYtelserOgAldersovergang: Regel<InnvilgedeMaanederGrunnlag, List<YtelseFoerAvkorting>> =
    RegelMeta(
        OMS_GYLDIG_FRA,
        "",
        RegelReferanse("", ""),
    ) benytter perioderMedYtelse og aldersovergang med { beregningsperioder, aldersovergang ->
        if (aldersovergang == null) {
            beregningsperioder
        } else {
            val perioder = beregningsperioder.filter { it.periode.fom < aldersovergang }
            if (perioder.isNotEmpty()) {
                val sistePeriode = perioder.last()

                perioder.dropLast(1) +
                    sistePeriode.copy(
                        periode =
                            sistePeriode.periode.copy(
                                tom =
                                    aldersovergang.minusMonths(
                                        1L,
                                    ),
                            ),
                    )
            } else {
                perioder
            }
        }
    }

val antallInnvilgedeMaanederForAar: Regel<InnvilgedeMaanederGrunnlag, Long> =
    RegelMeta(
        OMS_GYLDIG_FRA,
        "",
        RegelReferanse("", ""),
    ) benytter perioderMedYtelserOgAldersovergang med { beregningsperioder ->
        beregningsperioder.sumOf { beregningsperiode ->
            if (beregningsperiode.beregning > 0) {
                beregningsperiode.periode.antallMaanederIPeriode()
            } else {
                0L
            }
        }
    }

fun Periode.antallMaanederIPeriode(): Long {
    val tomEllerDesember =
        this.tom ?: YearMonth.of(
            this.fom.year,
            Month.DECEMBER,
        )

    return this.fom.until(
        tomEllerDesember,
        ChronoUnit.MONTHS,
    ) + 1L
}
