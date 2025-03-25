package no.nav.etterlatte.avkorting

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.avkorting.AvkortetYtelseType.AARSOPPGJOER
import no.nav.etterlatte.avkorting.AvkortetYtelseType.ETTEROPPJOER
import no.nav.etterlatte.avkorting.AvkortetYtelseType.FORVENTET_INNTEKT
import no.nav.etterlatte.avkorting.AvkortingRegelkjoring.beregnInntektInnvilgetPeriodeFaktiskInntekt
import no.nav.etterlatte.avkorting.AvkortingRegelkjoring.beregnInntektInnvilgetPeriodeForventetInntekt
import no.nav.etterlatte.beregning.Beregning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.AvkortingFrontend
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagFrontend
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.beregning.SanksjonertYtelse
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.sanksjon.Sanksjon
import java.time.Month
import java.time.YearMonth
import java.util.UUID

data class Avkorting(
    val aarsoppgjoer: List<Aarsoppgjoer> = emptyList(),
) {
    init {
        if (!aarsoppgjoer.zipWithNext().all { it.first.aar < it.second.aar }) {
            throw InternfeilException("Årsoppgjør er i feil rekkefølge.")
        }
    }

    /*
     * Å skille på år er kun relevant internt for avkorting. Alle perioder på tvers av alle årene blir
     * derfor flatet ut til sammenhengende perioder når avkorting hentes ut.
     *
     * For AvkortingGrunnlag (inntekter) så er det ønskelig med full historikk. // TODO er det fortsatt det???
     * For AvkortetYtelse er det kun ønskelig å hente fra og med virkningstidspunkt til behandling.
     *
     * avkortetYtelseForrigeVedtak - brukes til å sammenligne med beløper til nye beregna perioder i frontend
     */
    fun toDto(fraVirkningstidspunkt: YearMonth? = null): AvkortingDto =
        AvkortingDto(
            avkortingGrunnlag =
                aarsoppgjoer.flatMap { aarsoppgjoer ->
                    when (aarsoppgjoer) {
                        is AarsoppgjoerLoepende ->
                            aarsoppgjoer.inntektsavkorting.map {
                                it.grunnlag.toDto()
                            }

                        is Etteroppgjoer -> {
                            TODO()
                        }
                    }
                },
            avkortetYtelse = utflatetLoependeYtelseEtterAvkorting(fraVirkningstidspunkt),
        )

    fun toFrontend(
        fraVirkningstidspunkt: YearMonth? = null,
        forrigeAvkorting: Avkorting? = null,
        behandlinStatus: BehandlingStatus? = null,
    ): AvkortingFrontend =
        AvkortingFrontend(
            avkortingGrunnlag =
                aarsoppgjoer.map { etAarsoppgjoer ->
                    when (etAarsoppgjoer) {
                        is AarsoppgjoerLoepende -> {
                            val avkortingGrunnlag =
                                etAarsoppgjoer.inntektsavkorting
                                    .map { inntektsavkorting -> inntektsavkorting.grunnlag.toDto() }
                                    .sortedByDescending { inntektsavkorting -> inntektsavkorting.fom }

                            val fraVirk = avkortingGrunnlag.singleOrNull { it.fom == fraVirkningstidspunkt }
                            AvkortingGrunnlagFrontend(
                                aar = etAarsoppgjoer.aar,
                                fraVirk = fraVirk,
                                historikk =
                                    avkortingGrunnlag.filter { it.id != fraVirk?.id },
                            )
                        }

                        is Etteroppgjoer -> TODO()
                    }
                },
            avkortetYtelse = utflatetLoependeYtelseEtterAvkorting(fraVirkningstidspunkt),
            tidligereAvkortetYtelse =
                forrigeAvkorting?.let { forrige ->
                    when (behandlinStatus) {
                        BehandlingStatus.IVERKSATT, null -> null
                        else -> {
                            forrige.aarsoppgjoer
                                .flatMap { it.avkortetYtelse }
                                .map { it.toDto() }
                        }
                    }
                } ?: emptyList(),
        )

    private fun utflatetLoependeYtelseEtterAvkorting(fraVirkningstidspunkt: YearMonth? = null): List<AvkortetYtelseDto> =
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

    /*
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
                                        inntektsavkorting.copy(
                                            grunnlag =
                                                inntektsavkorting.grunnlag.copy(
                                                    id = UUID.randomUUID(),
                                                    periode =
                                                        inntektsavkorting.grunnlag.periode.copy(
                                                            fom = inntektsavkorting.grunnlag.periode.fom,
                                                            tom =
                                                                inntektsavkorting.grunnlag.periode.tom
                                                                    ?: opphoerFom?.let {
                                                                        tomOpphoerFomEllerAarsskifte(
                                                                            inntektsavkorting.grunnlag.periode.fom,
                                                                            opphoerFom,
                                                                        )
                                                                    },
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

    fun beregnAvkortingMedNyttGrunnlag(
        nyttGrunnlag: AvkortingGrunnlagLagreDto,
        bruker: BrukerTokenInfo,
        beregning: Beregning?,
        sanksjoner: List<Sanksjon>,
        opphoerFom: YearMonth?,
        aldersovergang: YearMonth? = null,
    ): Avkorting {
        val oppdatertMedNyInntekt = oppdaterMedInntektsgrunnlag(nyttGrunnlag, bruker, opphoerFom, aldersovergang)
        return oppdatertMedNyInntekt.beregnAvkorting(nyttGrunnlag.fom, beregning, sanksjoner)
    }

    private fun finnTom(
        opphoerFom: YearMonth? = null,
        nyttGrunnlag: AvkortingGrunnlagLagreDto,
    ): YearMonth? =
        // opphoerFom kan maks være ett år frem
        opphoerFom?.let {
            if (it.year.minus(nyttGrunnlag.fom.year) == 1) {
                YearMonth.of(nyttGrunnlag.fom.year, Month.DECEMBER)
            } else if (it.year == nyttGrunnlag.fom.year) {
                it.minusMonths(1)
            } else {
                throw OpphoerErTilbakeITid(
                    "Opphøer er tilbake i tid, opphør: $opphoerFom nyttgrunnlag er fra: ${nyttGrunnlag.fom} id: ${nyttGrunnlag.id}",
                )
            }
        }

    fun oppdaterMedInntektsgrunnlag(
        nyttGrunnlag: AvkortingGrunnlagLagreDto,
        bruker: BrukerTokenInfo,
        opphoerFom: YearMonth? = null,
        aldersovergang: YearMonth? = null,
    ): Avkorting {
        val aarsoppgjoer =
            hentEllerOpprettAarsoppgjoer(nyttGrunnlag.fom) as? AarsoppgjoerLoepende
                ?: throw InternfeilException("Kan ikke oppdatere inntektsgrunnlag for et år som har etteroppgjør")

        val tom = finnTom(opphoerFom, nyttGrunnlag)

        val inntektTom = nyttGrunnlag.inntektTom
        val fratrekkInnAar = nyttGrunnlag.fratrekkInnAar
        val inntektUtlandTom = nyttGrunnlag.inntektUtlandTom
        val fratrekkInnAarUtland = nyttGrunnlag.fratrekkInnAarUtland
        val kilde = Grunnlagsopplysning.Saksbehandler(bruker.ident(), Tidspunkt.now())
        val periode = Periode(fom = nyttGrunnlag.fom, tom = tom)

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
                        ?: finnAntallInnvilgaMaanederForAar(aarsoppgjoer.fom, tom, aldersovergang),
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
                // Fjerner hvis det finnes fra før for å erstatte/redigere
                .filter { it.grunnlag.id != nyttGrunnlag.id }
                .map {
                    it.lukkSisteInntektsperiode(nyttGrunnlag.fom, tom)
                }.plus(Inntektsavkorting(grunnlag = forventetInntekt))

        val oppdatertAarsoppjoer =
            aarsoppgjoer.copy(
                inntektsavkorting = oppdatert,
            )
        return this.copy(
            aarsoppgjoer = erstattAarsoppgjoer(oppdatertAarsoppjoer),
        )
    }

    fun beregnEtteroppgjoer(
        brukerTokenInfo: BrukerTokenInfo,
        aar: Int,
        loennsinntekt: Int,
        afp: Int,
        naeringsinntekt: Int,
        utland: Int,
        sanksjoner: List<Sanksjon>,
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
                inntektInnvilgetPeriode =
                    beregnInntektInnvilgetPeriodeFaktiskInntekt(
                        loennsinntekt = loennsinntekt,
                        afp = afp,
                        naeringsinntekt = naeringsinntekt,
                        utland = utland,
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
        val nyAvkorting =
            copy(
                aarsoppgjoer = erstattAarsoppgjoer(etteroppgjoer),
            )
        return nyAvkorting.beregnAvkorting(tidligereAarsoppgjoer.fom, null, sanksjoner)
    }

    fun beregnAvkorting(
        virkningstidspunkt: YearMonth,
        beregning: Beregning?, // Kun null for forbehandling etteroppgjør
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
                    is AarsoppgjoerLoepende -> {
                        val reberegnetInntektsavkorting =
                            aarsoppgjoer.inntektsavkorting.map { inntektsavkorting ->
                                val periode =
                                    Periode(
                                        fom = aarsoppgjoer.fom,
                                        tom = inntektsavkorting.grunnlag.periode.tom,
                                    )

                                val avkortinger =
                                    AvkortingRegelkjoring.beregnInntektsavkorting(
                                        periode = periode,
                                        avkortingGrunnlag = inntektsavkorting.grunnlag,
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
                                    val tomDesember = tomHvisInntektNesteAar(aarsoppgjoer)
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

                        aarsoppgjoer.copy(
                            ytelseFoerAvkorting = ytelseFoerAvkorting,
                            inntektsavkorting = reberegnetInntektsavkorting,
                            avkortetYtelse = avkortetYtelse,
                        )
                    }

                    is Etteroppgjoer -> {
                        val avkortinger =
                            AvkortingRegelkjoring.beregnInntektsavkorting(
                                periode = aarsoppgjoer.inntekt.periode,
                                avkortingGrunnlag = aarsoppgjoer.inntekt,
                            )

                        val avkortetYtelseFaktiskInntekt =
                            AvkortingRegelkjoring.beregnAvkortetYtelse(
                                periode = aarsoppgjoer.inntekt.periode,
                                ytelseFoerAvkorting = ytelseFoerAvkorting,
                                avkortingsperioder = avkortinger,
                                type = ETTEROPPJOER,
                                restanse = null,
                                sanksjoner = sanksjoner,
                            )

                        aarsoppgjoer.copy(
                            ytelseFoerAvkorting = ytelseFoerAvkorting,
                            avkortingsperioder = avkortinger,
                            avkortetYtelse = avkortetYtelseFaktiskInntekt,
                        )
                    }
                }
            }

        return copy(aarsoppgjoer = oppdaterteOppgjoer)
    }

    /**
     * Finner avkortet ytelse med opparbeidet [Restanse]
     * Opparbeidet restanse beregnes ved å sammenligne samtlige forventa inntektsavkortinger med alle måneder frem til
     * ny oppgitt forventet inntekt.
     * For hver forventet inntekt som sammenlignes så akkumuleres det mer eller mindre restanse.
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

        val tomDesember = tomHvisInntektNesteAar(aarsoppgjoer)

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

    private fun tomHvisInntektNesteAar(aarsoppgjoer: AarsoppgjoerLoepende): YearMonth? {
        val harAaroppgjoerNesteAaar =
            this.aarsoppgjoer.any { it.aar == aarsoppgjoer.aar + 1 }
        return when (harAaroppgjoerNesteAaar) {
            true -> YearMonth.of(aarsoppgjoer.aar, 12)
            false -> null
        }
    }

    private fun tomOpphoerFomEllerAarsskifte(
        fom: YearMonth,
        opphoerFom: YearMonth,
    ) = if (opphoerFom.year > fom.year) {
        YearMonth.of(fom.year, Month.DECEMBER)
    } else {
        opphoerFom.minusMonths(1)
    }

    /*
     * Hvilket årsoppgjør som er relevant basers på virkningstidspunkt.
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

    private fun erstattAarsoppgjoer(nytt: Aarsoppgjoer): List<Aarsoppgjoer> {
        if (aarsoppgjoer.any { it.aar == nytt.aar }) {
            return aarsoppgjoer.map { if (it.aar == nytt.aar) nytt else it }
        }
        return (aarsoppgjoer + listOf(nytt)).sortedBy { it.aar }
    }
}

class OpphoerErTilbakeITid(
    msg: String,
) : InternfeilException(msg)

sealed class AvkortingGrunnlag {
    abstract val id: UUID
    abstract val periode: Periode
    abstract val innvilgaMaaneder: Int
    abstract val inntektInnvilgetPeriode: InntektInnvilgetPeriode
    abstract val kilde: Grunnlagsopplysning.Saksbehandler
}

data class ForventetInntekt(
    override val id: UUID,
    override val periode: Periode,
    val inntektTom: Int,
    val fratrekkInnAar: Int,
    val inntektUtlandTom: Int,
    val fratrekkInnAarUtland: Int,
    override val innvilgaMaaneder: Int,
    val spesifikasjon: String,
    override val kilde: Grunnlagsopplysning.Saksbehandler,
    val overstyrtInnvilgaMaanederAarsak: OverstyrtInnvilgaMaanederAarsak? = null,
    val overstyrtInnvilgaMaanederBegrunnelse: String? = null,
    override val inntektInnvilgetPeriode: InntektInnvilgetPeriode,
) : AvkortingGrunnlag()

data class FaktiskInntekt(
    override val id: UUID,
    override val periode: Periode,
    override val innvilgaMaaneder: Int,
    val loennsinntekt: Int,
    val naeringsinntekt: Int,
    val afp: Int,
    val utlandsinntekt: Int,
    override val kilde: Grunnlagsopplysning.Saksbehandler,
    override val inntektInnvilgetPeriode: BenyttetInntektInnvilgetPeriode,
) : AvkortingGrunnlag()

sealed class InntektInnvilgetPeriode

data class BenyttetInntektInnvilgetPeriode(
    val verdi: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode,
    val kilde: Grunnlagsopplysning.RegelKilde,
) : InntektInnvilgetPeriode()

/*
 Benyttes for behandlinger som allerede er iverksatt med gammel regelkode hvor InntektInnvilgetPeriode ikke var et konsept
 Se historikk til regelkode for avkorting for mer info.
*/
data object IngenInntektInnvilgetPeriode : InntektInnvilgetPeriode()

enum class OverstyrtInnvilgaMaanederAarsak {
    TAR_UT_PENSJON_TIDLIG,
    BLIR_67,
    ANNEN,
}

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
            val overlapperTom = sanksjon.fom.year <= aar && (sanksjon.tom == null || sanksjon.tom.year >= aar)
            erFomDetteAaret || overlapperTom
        }
}

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
 * [avkortingsperioder] utregnet basert på en årsinntekt ([grunnlag]).
 *
 * [avkortetYtelseForventetInntekt] - Benyttes hvis forventet årsinntekt endrer seg i løpet av året for å finne
 * restansen (se [Avkorting.beregnAvkortetYtelseMedRestanse]). Vil da inneholde ytelse etter avkorting slik
 * den ville vært med denne årsinntekten.
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

fun finnAntallInnvilgaMaanederForAar(
    aarsoppgjoerFom: YearMonth,
    tom: YearMonth?,
    aldersovergang: YearMonth?,
): Int {
    val tomMaaned = tom ?: aldersovergang?.minusMonths(1)
    val tomEllerDesember = tomMaaned?.monthValue ?: 12
    return tomEllerDesember - (aarsoppgjoerFom.monthValue - 1)
}
