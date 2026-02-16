package no.nav.etterlatte.avkorting.regler

import no.nav.etterlatte.avkorting.MaanedInnvilget
import no.nav.etterlatte.avkorting.YtelseFoerAvkorting
import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FRA
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import java.time.YearMonth

data class MaanederInnvilgetGrunnlag(
    val beregningsperioder: FaktumNode<List<YtelseFoerAvkorting>>,
    val tilOgMed: FaktumNode<YearMonth?>,
)

val perioderMedYtelse =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Beregningsperiodene i årsoppgjøret",
        finnFaktum = MaanederInnvilgetGrunnlag::beregningsperioder,
        finnFelt = { it },
    )

val tilOgMed =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "En til-og-med-dato satt for året",
        finnFaktum = MaanederInnvilgetGrunnlag::tilOgMed,
        finnFelt = { it },
    )

val ytelsesperioderJustertForTilOgMed: Regel<MaanederInnvilgetGrunnlag, List<YtelseFoerAvkorting>> =
    RegelMeta(
        OMS_GYLDIG_FRA,
        "Finner de relevante ytelsesperiodene avhengig av om vi har en til-og-med-dato",
        RegelReferanse("JUSTERTE-BEREGNINGSPERIODER", "1.0"),
    ) benytter perioderMedYtelse og tilOgMed med { beregningsperioder, tilOgMed ->
        if (tilOgMed == null) {
            beregningsperioder
        } else {
            val perioderFoerTilOgMed = beregningsperioder.filter { it.periode.fom <= tilOgMed }
            if (perioderFoerTilOgMed.isNotEmpty()) {
                val sistePeriode = perioderFoerTilOgMed.last()
                val sistePeriodeTilOgMed = sistePeriode.periode.tom

                if (sistePeriodeTilOgMed != null && sistePeriodeTilOgMed < tilOgMed) {
                    perioderFoerTilOgMed
                } else {
                    perioderFoerTilOgMed.dropLast(1) +
                        sistePeriode.copy(
                            periode =
                                sistePeriode.periode.copy(
                                    tom = tilOgMed,
                                ),
                        )
                }
            } else {
                perioderFoerTilOgMed
            }
        }
    }

val alleMaanederIAaret =
    RegelMeta(
        OMS_GYLDIG_FRA,
        "Gir en liste av alle månedene i året",
        RegelReferanse("MAANEDER-I-AARET", "1.0"),
    ) benytter perioderMedYtelse med { perioder ->
        val aar =
            perioder
                .first()
                .periode.fom.year
        (1..12).map { YearMonth.of(aar, it) }
    }

val erMaanederForAaretInnvilget: Regel<MaanederInnvilgetGrunnlag, List<MaanedInnvilget>> =
    RegelMeta(
        OMS_GYLDIG_FRA,
        "Finner om hver måned i året har en innvilget ytelse > 0",
        RegelReferanse("MAANEDER-INNVILGET-MED-YTELSE", "1.0"),
    ) benytter ytelsesperioderJustertForTilOgMed og alleMaanederIAaret med { beregningsperioder, alleMaaneder ->
        alleMaaneder.map { maaned ->
            val innvilget =
                beregningsperioder.any {
                    it.periode.erMaanedIPerioden(maaned) && it.beregning > 0
                }
            MaanedInnvilget(
                maaned = maaned,
                innvilget = innvilget,
            )
        }
    }

fun Periode.erMaanedIPerioden(maaned: YearMonth): Boolean = this.fom <= maaned && (this.tom ?: maaned) >= maaned
