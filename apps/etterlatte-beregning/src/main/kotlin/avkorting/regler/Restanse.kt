package no.nav.etterlatte.avkorting.regler

import no.nav.etterlatte.avkorting.MaanedInnvilget
import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FRA
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
    val fraOgMedNyForventetInntekt: FaktumNode<YearMonth>,
    val maanederOgSanksjon: FaktumNode<List<Pair<YearMonth, Boolean>>>,
    val maanederInnvilget: FaktumNode<List<MaanedInnvilget>>,
)

val maanederInnvilget: Regel<RestanseGrunnlag, List<MaanedInnvilget>> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        finnFaktum = RestanseGrunnlag::maanederInnvilget,
        finnFelt = { it },
    )

val tidligereYtelse: Regel<RestanseGrunnlag, List<Int>> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Finner tidligere ytelse etter avkorting",
        finnFaktum = RestanseGrunnlag::tidligereYtelseEtterAvkorting,
        finnFelt = { it },
    )

val nyYtelse: Regel<RestanseGrunnlag, List<Int>> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Finner ny forventet ytelse etter avkorting",
        finnFaktum = RestanseGrunnlag::nyForventetYtelseEtterAvkorting,
        finnFelt = { it },
    )
val virkningstidspunkt: Regel<RestanseGrunnlag, YearMonth> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Virkningstidspunkt til restanse skal gjelde fra",
        finnFaktum = RestanseGrunnlag::fraOgMedNyForventetInntekt,
        finnFelt = { it },
    )

val totalRestanse =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Beregner restanse etter endret ytelse etter avkorting på grunn av endret årsinntekt",
        regelReferanse = RegelReferanse("TOTAL-RESTANSE-INNTEKTSENDRING"),
    ) benytter tidligereYtelse og nyYtelse med { tidligereYtelse, nyYtelse ->
        Beregningstall(tidligereYtelse.sum()).minus(Beregningstall(nyYtelse.sum())).toInteger()
    }

val perioderMedSanksjonGrunnlag =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Finner ny forventet ytelse etter avkorting",
        finnFaktum = RestanseGrunnlag::maanederOgSanksjon,
        finnFelt = { it },
    )

val maanederMedSanksjonEtterVirk =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Teller antall måneder vi har sanksjon etter virk",
        regelReferanse = RegelReferanse("MAANEDER-MED-SANKSJON-ETTER-VIRK"),
    ) benytter virkningstidspunkt og perioderMedSanksjonGrunnlag med { virkningstidspunkt, perioder ->
        perioder
            .filter { it.first >= virkningstidspunkt }
            .count { it.second }
    }

val gjenvaerendeMaaneder =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Beregner hvor mange måneder som gjenstår i gjeldende år fra nytt virkningstidspunkt, ekskludert sanksjoner",
        regelReferanse = RegelReferanse(id = "GJENVAERENDE-MAANEDER-FOR-FORDELT-RESTANSE", versjon = "2"),
    ) benytter virkningstidspunkt og maanederMedSanksjonEtterVirk med { virkningstidspunkt, maaneder ->
        Beregningstall(12)
            .minus(Beregningstall(virkningstidspunkt.monthValue))
            .minus(Beregningstall(maaneder))
            .plus(Beregningstall(1))
    }

val gjenvaerendeMaaneder_v2 =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse =
            "Beregner hvor mange måneder som gjenstår i gjeldende år fra nytt " +
                "virkningstidspunkt, ekskludert måneder vi ikke er innvilget",
        regelReferanse = RegelReferanse(id = "GJENVAERENDE-MAANEDER-FOR-FORDELT-RESTANSE", versjon = "3.0"),
    ) benytter virkningstidspunkt og maanederInnvilget med { virk, maaneder ->
        maaneder.count { it.innvilget && it.maaned >= virk }
    }

val fordeltRestanse =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Fordeler oppsummert restanse over gjenværende måneder av gjeldende år",
        regelReferanse = RegelReferanse("FORDELT-RESTANSE-INNTEKTSENDRING", versjon = "2"),
    ) benytter totalRestanse og gjenvaerendeMaaneder med { sumRestanse, gjenvaerendeMaaneder ->
        if (gjenvaerendeMaaneder.toInteger() == 0) {
            sumRestanse
        } else {
            Beregningstall(sumRestanse).divide(gjenvaerendeMaaneder).toInteger()
        }
    }

val fordeltRestanse_v2 =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Fordeler oppsummert restanse over gjenværende måneder av gjeldende år",
        regelReferanse = RegelReferanse("FORDELT-RESTANSE-INNTEKTSENDRING", versjon = "2"),
    ) benytter totalRestanse og gjenvaerendeMaaneder_v2 med { sumRestanse, gjenvaerendeMaaneder ->
        if (gjenvaerendeMaaneder == 0) {
            sumRestanse
        } else {
            Beregningstall(sumRestanse).divide(gjenvaerendeMaaneder).toInteger()
        }
    }

val restanse =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Beregner restanse etter endret ytelse etter avkorting på grunn av endret årsinntekt",
        regelReferanse = RegelReferanse("RESTANSE-INNTEKTSENDRING"),
    ) benytter totalRestanse og fordeltRestanse med { totalRestanse, fordeltRestanse ->
        totalRestanse to fordeltRestanse
    }

val restanse_v2 =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Beregner restanse etter endret ytelse etter avkorting på grunn av endret årsinntekt",
        regelReferanse = RegelReferanse("RESTANSE-INNTEKTSENDRING", versjon = "2"),
    ) benytter totalRestanse og fordeltRestanse_v2 med { totalRestanse, fordeltRestanse ->
        totalRestanse to fordeltRestanse
    }
