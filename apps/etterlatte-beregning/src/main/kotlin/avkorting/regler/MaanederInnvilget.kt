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
        beskrivelse = "",
        finnFaktum = MaanederInnvilgetGrunnlag::beregningsperioder,
        finnFelt = { it },
    )

val tilOgMed =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        finnFaktum = MaanederInnvilgetGrunnlag::tilOgMed,
        finnFelt = { it },
    )

val perioderMedYtelserOgAldersovergang: Regel<MaanederInnvilgetGrunnlag, List<YtelseFoerAvkorting>> =
    RegelMeta(
        OMS_GYLDIG_FRA,
        "",
        RegelReferanse("", ""),
    ) benytter perioderMedYtelse og tilOgMed med { beregningsperioder, tilOgMed ->
        if (tilOgMed == null) {
            beregningsperioder
        } else {
            val perioder = beregningsperioder.filter { it.periode.fom < tilOgMed }
            if (perioder.isNotEmpty()) {
                val sistePeriode = perioder.last()

                perioder.dropLast(1) +
                    sistePeriode.copy(
                        periode =
                            sistePeriode.periode.copy(
                                tom = tilOgMed,
                            ),
                    )
            } else {
                perioder
            }
        }
    }

val alleMaanederIAaret =
    RegelMeta(
        OMS_GYLDIG_FRA,
        "",
        RegelReferanse("", ""),
    ) benytter perioderMedYtelse med { perioder ->
        val aar =
            perioder
                .first()
                .periode.fom.year
        (1..12).map { YearMonth.of(aar, it) }
    }

val antallInnvilgedeMaanederForAar: Regel<MaanederInnvilgetGrunnlag, List<MaanedInnvilget>> =
    RegelMeta(
        OMS_GYLDIG_FRA,
        "",
        RegelReferanse("", ""),
    ) benytter perioderMedYtelserOgAldersovergang og alleMaanederIAaret med { beregningsperioder, alleMaaneder ->
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
