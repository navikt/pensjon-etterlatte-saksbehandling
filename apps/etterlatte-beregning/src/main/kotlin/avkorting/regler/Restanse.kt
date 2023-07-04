package no.nav.etterlatte.avkorting.regler

import no.nav.etterlatte.avkorting.Restanse
import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FROM_TEST
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import no.nav.etterlatte.regler.Beregningstall
import java.time.YearMonth

data class RestanseGrunnlag(
    val tidligereYtelseEtterAvkorting: FaktumNode<List<Int>>,
    val nyForventetYtelseEtterAvkorting: FaktumNode<List<Int>>,
    val virkningstidspunkt: FaktumNode<YearMonth>,
)

val tidligereYtelse: Regel<RestanseGrunnlag, List<Int>> = finnFaktumIGrunnlag(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Finner tidligere ytelse etter avkorting",
    finnFaktum = RestanseGrunnlag::tidligereYtelseEtterAvkorting,
    finnFelt = { it }
)

val nyYtelse: Regel<RestanseGrunnlag, List<Int>> = finnFaktumIGrunnlag(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Finner ny forventet ytelse etter avkorting",
    finnFaktum = RestanseGrunnlag::nyForventetYtelseEtterAvkorting,
    finnFelt = { it }
)
val virkningstidspunkt: Regel<RestanseGrunnlag, YearMonth> = finnFaktumIGrunnlag(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Virkningstidspunkt til restanse skal gjelde fra",
    finnFaktum = RestanseGrunnlag::virkningstidspunkt,
    finnFelt = { it }
)

val totalRestanse = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Beregner restanse etter endret ytelse etter avkorting på grunn av endret årsinntekt",
    regelReferanse = RegelReferanse("RESTANSE-INNTEKTSENDRING")
) benytter tidligereYtelse og nyYtelse med { tidligereYtelse, nyYtelse ->
    Beregningstall(tidligereYtelse.sum()).minus(Beregningstall(nyYtelse.sum())).toInteger()
}

val gjenvaerendeMaaneder = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Beregner hvor mange måneder som gjenstår i gjeldende år fra nytt virkningstidspunkt",
    regelReferanse = RegelReferanse("GJENVAERENDE-MAANEDER-FOR-FORDELT-RESTANSE")
) benytter virkningstidspunkt med { virkningstidspunkt ->
    Beregningstall(12)
        .minus(Beregningstall(virkningstidspunkt.monthValue))
        .plus(Beregningstall(1))
}

val fordeltRestanse = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Fordeler oppsummert restanse over gjenværende måneder av gjeldende år",
    regelReferanse = RegelReferanse("FORDELT-RESTANSE-INNTEKTSENDRING")
) benytter totalRestanse og gjenvaerendeMaaneder med { sumRestanse, gjenvaerendeMaaneder ->
    Beregningstall(sumRestanse).divide(gjenvaerendeMaaneder).toInteger()
}

val restanse = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Beregner restanse etter endret ytelse etter avkorting på grunn av endret årsinntekt",
    regelReferanse = RegelReferanse("RESTANSE-INNTEKTSENDRING")
) benytter totalRestanse og fordeltRestanse med { totalRestanse, fordeltRestanse ->
    Restanse(
        totalRestanse = totalRestanse,
        fordeltRestanse = fordeltRestanse
    )
}