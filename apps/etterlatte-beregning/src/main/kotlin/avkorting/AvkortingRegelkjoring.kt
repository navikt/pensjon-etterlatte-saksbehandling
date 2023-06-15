package no.nav.etterlatte.avkorting

import no.nav.etterlatte.avkorting.regler.InntektAvkortingGrunnlag
import no.nav.etterlatte.avkorting.regler.PeriodisertAvkortetYtelseGrunnlag
import no.nav.etterlatte.avkorting.regler.PeriodisertInntektAvkortingGrunnlag
import no.nav.etterlatte.avkorting.regler.avkorteYtelse
import no.nav.etterlatte.avkorting.regler.kroneavrundetInntektAvkorting
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.PeriodisertBeregningGrunnlag
import no.nav.etterlatte.beregning.grunnlag.mapVerdier
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.PeriodisertGrunnlag
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import no.nav.etterlatte.regler.Beregningstall
import okhttp3.internal.toImmutableList
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.time.YearMonth

object AvkortingRegelkjoring {

    private val logger = LoggerFactory.getLogger(AvkortingRegelkjoring::class.java)

    fun beregnInntektsavkorting(
        periode: Periode,
        avkortingGrunnlag: List<AvkortingGrunnlag>
    ): List<Avkortingsperiode> {
        logger.info("Beregner inntektsavkorting")

        val grunnlag = PeriodisertInntektAvkortingGrunnlag(
            periodisertInntektAvkortingGrunnlag = PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
                avkortingGrunnlag.map {
                    GrunnlagMedPeriode(
                        data = it,
                        fom = it.periode.fom.atDay(1),
                        tom = it.periode.tom?.atEndOfMonth()
                    )
                }.mapVerdier {
                    FaktumNode(
                        verdi = InntektAvkortingGrunnlag(
                            inntekt = Beregningstall(it.aarsinntekt),
                            fratrekkInnUt = Beregningstall(it.fratrekkInnAar),
                            relevanteMaaneder = Beregningstall(it.relevanteMaanederInnAar)
                        ),
                        kilde = it.kilde,
                        beskrivelse = "Forventet årsinntekt"
                    )
                }
            ) { _, _, _ -> throw IllegalArgumentException("Noe gikk galt ved uthenting av periodiserte beregninger") }
        )

        val resultat = kroneavrundetInntektAvkorting.eksekver(grunnlag, periode.tilRegelPeriode())
        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val tidspunkt = Tidspunkt.now()
                resultat.periodiserteResultater.map { periodisertResultat ->
                    Avkortingsperiode(
                        periode = Periode(
                            fom = YearMonth.from(periodisertResultat.periode.fraDato),
                            tom = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) }
                        ),
                        avkorting = periodisertResultat.resultat.verdi,
                        tidspunkt = tidspunkt,
                        regelResultat = periodisertResultat.toJsonNode(),
                        kilde = Grunnlagsopplysning.RegelKilde(
                            navn = kroneavrundetInntektAvkorting.regelReferanse.id,
                            ts = tidspunkt,
                            versjon = periodisertResultat.reglerVersjon
                        )
                    )
                }
            }

            is RegelkjoeringResultat.UgyldigPeriode ->
                throw RuntimeException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
        }
    }

    fun beregnAarsoppgjoer(
        avkorting: Avkorting,
        virkningstidspunkt: YearMonth,
        forrigeAvkorting: Avkorting? = null
    ): List<Aarsoppgjoer> {
        // TODO EY-2368 - Lag regler og grunnlag

        val nyttAvkortingsgrunnlag = avkorting.hentAktiveInntektsgrunnlag()
        val fraForsteVirkEllerStartenAvAr = Periode(
            fom = YearMonth.of(virkningstidspunkt.year, 12 - nyttAvkortingsgrunnlag.relevanteMaanederInnAar + 1),
            tom = null
        )
        val avkortingNyInntekt = beregnInntektsavkorting(
            periode = fraForsteVirkEllerStartenAvAr,
            listOf(nyttAvkortingsgrunnlag.copy(periode = fraForsteVirkEllerStartenAvAr))
        )

        val forrigeAvkortingsmaaneder =
            forrigeAvkorting?.aarsoppgjoer?.associate { it.maaned to it.nyAvkorting }
                ?: avkorting.aarsoppgjoer.associate { it.maaned to it.tidligereAvkorting }

        val gjenvaerendeMaaneder = (12 - virkningstidspunkt.monthValue + 1)
        val maanedligRestanse = forrigeAvkortingsmaaneder.filterKeys { it < virkningstidspunkt }
            .map { (maaned, forrigeAvkorting) ->
                avkortingNyInntekt.avkortingIPeriode(maaned) - forrigeAvkorting
            }.sum() / gjenvaerendeMaaneder

        val nyttOppgjoer = mutableListOf<Aarsoppgjoer>()
        for (i in 1..12) {
            val maaned = YearMonth.of(virkningstidspunkt.year, i)
            if (maaned < fraForsteVirkEllerStartenAvAr.fom) continue

            val avkortingForventetInntekt = avkortingNyInntekt.avkortingIPeriode(maaned)
            val tidligereAvkorting = forrigeAvkortingsmaaneder[maaned] ?: 0
            val restanse = if (maaned < virkningstidspunkt) 0 else maanedligRestanse
            val nyAvkorting =
                if (maaned < virkningstidspunkt) tidligereAvkorting else avkortingForventetInntekt + maanedligRestanse

            nyttOppgjoer.add(
                Aarsoppgjoer(
                    maaned = maaned,
                    avkortingForventetInntekt = avkortingForventetInntekt,
                    tidligereAvkorting = tidligereAvkorting,
                    restanse = restanse,
                    nyAvkorting = nyAvkorting
                )
            )
        }
        return nyttOppgjoer.toImmutableList()
    }

    fun beregnAvkortetYtelse(
        periode: Periode,
        beregningsperioder: List<Beregningsperiode>,
        avkortingsperioder: List<Avkortingsperiode>,
        maanedligRestanse: Int // TODO EY-2368 - må gjøres om til grunnlag
    ): List<AvkortetYtelse> {
        val regelgrunnlag = PeriodisertAvkortetYtelseGrunnlag(
            beregningsperioder = periodiserteBeregninger(beregningsperioder),
            avkortingsperioder = periodiserteAvkortinger(avkortingsperioder)
        )
        val resultat = avkorteYtelse.eksekver(regelgrunnlag, periode.tilRegelPeriode())
        when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val tidspunkt = Tidspunkt.now()
                return resultat.periodiserteResultater.map { periodisertResultat ->
                    val resultatFom = periodisertResultat.periode.fraDato
                    AvkortetYtelse(
                        periode = Periode(
                            fom = YearMonth.from(periodisertResultat.periode.fraDato),
                            tom = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) }
                        ),
                        ytelseEtterAvkorting = periodisertResultat.resultat.verdi + maanedligRestanse,
                        restanse = maanedligRestanse,
                        avkortingsbeloep = regelgrunnlag.finnGrunnlagForPeriode(resultatFom).avkorting.verdi,
                        ytelseFoerAvkorting = regelgrunnlag.finnGrunnlagForPeriode(resultatFom).beregning.verdi,
                        tidspunkt = tidspunkt,
                        regelResultat = periodisertResultat.toJsonNode(),
                        kilde = Grunnlagsopplysning.RegelKilde(
                            navn = kroneavrundetInntektAvkorting.regelReferanse.id,
                            ts = tidspunkt,
                            versjon = periodisertResultat.reglerVersjon
                        )
                    )
                }
            }

            is RegelkjoeringResultat.UgyldigPeriode ->
                throw RuntimeException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
        }
    }

    private fun periodiserteBeregninger(beregninger: List<Beregningsperiode>): PeriodisertGrunnlag<FaktumNode<Int>> =
        PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
            beregninger.map {
                GrunnlagMedPeriode(
                    data = it,
                    fom = it.datoFOM.atDay(1),
                    tom = it.datoTOM?.atEndOfMonth()
                )
            }.mapVerdier {
                FaktumNode(
                    verdi = it.utbetaltBeloep,
                    kilde = it.kilde
                        ?: throw IllegalArgumentException("Noe gikk galt ved uthenting av periodiserte beregninger"),
                    beskrivelse = "Beregnet ytelse før avkorting for periode"
                )
            }
        ) { _, _, _ -> throw IllegalArgumentException("Noe gikk galt ved uthenting av periodiserte beregninger") }

    private fun periodiserteAvkortinger(avkortingGrunnlag: List<Avkortingsperiode>):
        PeriodisertGrunnlag<FaktumNode<Int>> =
        PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
            avkortingGrunnlag.map {
                GrunnlagMedPeriode(
                    data = it,
                    fom = it.periode.fom.atDay(1),
                    tom = it.periode.tom?.atEndOfMonth()
                )
            }.mapVerdier {
                FaktumNode(
                    verdi = it.avkorting,
                    kilde = it.kilde,
                    beskrivelse = "Beregnet avkorting for periode"
                )
            }
        ) { _, _, _ -> throw IllegalArgumentException("Noe gikk galt ved uthenting av periodiserte avkortinger") }
}

fun Periode.tilRegelPeriode(): RegelPeriode = RegelPeriode(
    fom.atDay(1),
    tom?.atEndOfMonth()
)