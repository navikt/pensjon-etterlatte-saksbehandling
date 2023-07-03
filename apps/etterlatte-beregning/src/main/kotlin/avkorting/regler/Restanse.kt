package no.nav.etterlatte.avkorting.regler

import no.nav.etterlatte.avkorting.AarsoppgjoerMaaned
import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FROM_TEST
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.PeriodisertGrunnlag
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import no.nav.etterlatte.regler.Beregningstall
import java.time.LocalDate
import java.time.YearMonth

data class PeriodisertRestanseGrunnlag(
    val tidligereYtelseEtterAvkorting: PeriodisertGrunnlag<FaktumNode<Int>>,
    val nyForventetYtelseEtterAvkorting: PeriodisertGrunnlag<FaktumNode<Int>>
) : PeriodisertGrunnlag<RestanseGrunnlag> {
    override fun finnAlleKnekkpunkter(): Set<LocalDate> =
        tidligereYtelseEtterAvkorting.finnAlleKnekkpunkter() + nyForventetYtelseEtterAvkorting.finnAlleKnekkpunkter()

    override fun finnGrunnlagForPeriode(datoIPeriode: LocalDate): RestanseGrunnlag =
        RestanseGrunnlag(
            tidligereYtelseEtterAvkorting = tidligereYtelseEtterAvkorting.finnGrunnlagForPeriode(datoIPeriode),
            nyForventetYtelseEtterAvkorting = nyForventetYtelseEtterAvkorting.finnGrunnlagForPeriode(datoIPeriode)
        )

}

data class RestanseGrunnlag(
    val tidligereYtelseEtterAvkorting: FaktumNode<Int>,
    val nyForventetYtelseEtterAvkorting: FaktumNode<Int>
)

val tidligereYtelse: Regel<RestanseGrunnlag, Int> = finnFaktumIGrunnlag(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Finner tidligere ytelse etter avkorting",
    finnFaktum = RestanseGrunnlag::tidligereYtelseEtterAvkorting,
    finnFelt = { it }
)

val nyYtelse: Regel<RestanseGrunnlag, Int> = finnFaktumIGrunnlag(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Finner ny forventet ytelse etter avkorting",
    finnFaktum = RestanseGrunnlag::nyForventetYtelseEtterAvkorting,
    finnFelt = { it }
)

val restanse = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Beregner restanse etter endret ytelse etter avkorting på grunn av endret årsinntekt",
    regelReferanse = RegelReferanse("RESTANSE-INNTEKTSENDRING") // TODO EY-2368 Confluence
) benytter tidligereYtelse og nyYtelse med { tidligereYtelse, nyYtelse ->
    Beregningstall(tidligereYtelse).minus(Beregningstall(nyYtelse)).toInteger()
}

data class FordelRestanseGrunnlag(
    val virkningstidspunkt: FaktumNode<YearMonth>,
    val maanederMedRestanse: FaktumNode<List<AarsoppgjoerMaaned>>
)

val virkningstidspunkt: Regel<FordelRestanseGrunnlag, YearMonth> = finnFaktumIGrunnlag(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Virkningstidspunkt til restanse skal gjelde fra",
    finnFaktum = FordelRestanseGrunnlag::virkningstidspunkt,
    finnFelt = { it }
)

// TODO EY-2368 kilde - Skal hele AarsoppgjoerMaaned med selv om bare restanse.verdi egentlig brukes? Gir kontekst?
val maanederMedRestanse: Regel<FordelRestanseGrunnlag, List<AarsoppgjoerMaaned>> = finnFaktumIGrunnlag(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Måneder hvor det har forekommet restanse",
    finnFaktum = FordelRestanseGrunnlag::maanederMedRestanse,
    finnFelt = { it }
)

val gjenvaerendeMaaneder = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Beregner hvor mange måneder som gjenstår i gjeldende år fra nytt virkningstidspunkt",
    regelReferanse = RegelReferanse("GJENVAERENDE-MAANEDER-FOR-FORDELT-RESTANSE")// TODO EY-2368 Confluence
) benytter virkningstidspunkt med { virkningstidspunkt ->
    Beregningstall(12)
        .minus(Beregningstall(virkningstidspunkt.monthValue))
        .plus(Beregningstall(1))
}

val sumRestanse = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Beregner summen av all restanse fra måneder med restanse",
    regelReferanse = RegelReferanse("SUMMERT-RESTANSE-INNTEKTSENDRING") // TODO EY-2368 Confluence
) benytter maanederMedRestanse med { maanederMedRestanse ->
    Beregningstall(maanederMedRestanse.sumOf {
        it.restanse
    })
}

val fordeltRestanse = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Fordeler oppsummert restanse over gjenværende måneder av gjeldende år",
    regelReferanse = RegelReferanse("FORDELT-RESTANSE-INNTEKTSENDRING") // TODO EY-2368 Confluence
) benytter sumRestanse og gjenvaerendeMaaneder med { sumRestanse, gjenvaerendeMaaneder ->
    sumRestanse.divide(gjenvaerendeMaaneder).toInteger()
}

