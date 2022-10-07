package barnepensjon.vilkaar.avdoedesmedlemskap

import barnepensjon.vilkaar.avdoedesmedlemskap.perioder.finnArbeidsforholdPerioder
import barnepensjon.vilkaar.avdoedesmedlemskap.perioder.finnArbeidsinntektPerioder
import barnepensjon.vilkaar.avdoedesmedlemskap.perioder.finnOffentligeYtelserPerioder
import barnepensjon.vilkaar.avdoedesmedlemskap.perioder.finnPensjonEllerTrygdePerioder
import barnepensjon.vilkaar.avdoedesmedlemskap.perioder.finnSaksbehandlerMedlemsPerioder
import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.barnepensjon.Periode
import no.nav.etterlatte.barnepensjon.hentGaps
import no.nav.etterlatte.barnepensjon.hentVurdering
import no.nav.etterlatte.barnepensjon.kombinerPerioder
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregResponse
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.hentArbeidsforhold
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentInntekt
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapVurdering
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Gap
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SaksbehandlerMedlemskapsperioder
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.VurdertMedlemskapsperiode
import no.nav.etterlatte.libs.common.inntekt.InntektsOpplysning
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun vilkaarAvdoedesMedlemskap(
    avdød: Grunnlagsdata<JsonNode>,
    saksbehandlerMedlemskapsPerioder: VilkaarOpplysning<SaksbehandlerMedlemskapsperioder>?
): VurdertVilkaar {
    val vilkaarstype = Vilkaartyper.AVDOEDES_FORUTGAAENDE_MEDLEMSKAP

    val arbeidsforholdOpplysning = avdød.hentArbeidsforhold() ?: return VurdertVilkaar.kanIkkeVurdere(vilkaarstype)
    val inntektsOpplysning = avdød.hentInntekt() ?: return VurdertVilkaar.kanIkkeVurdere(vilkaarstype)
    val doedsdato = avdød.hentDoedsdato()?.verdi ?: return VurdertVilkaar.kanIkkeVurdere(vilkaarstype)

    val bosattNorgeMetakriterie = metakriterieBosattNorge(avdød)

    val medlemskapOffentligOgInntektKriterie = kriterieMedlemskapOffentligOgInntekt(
        arbeidsforholdOpplysning,
        inntektsOpplysning,
        doedsdato,
        saksbehandlerMedlemskapsPerioder
    )

    val vurderingsResultat =
        hentVurdering(listOf(medlemskapOffentligOgInntektKriterie.resultat, bosattNorgeMetakriterie.resultat))

    return VurdertVilkaar(
        Vilkaartyper.AVDOEDES_FORUTGAAENDE_MEDLEMSKAP,
        vurderingsResultat,
        bosattNorgeMetakriterie.utfall,
        bosattNorgeMetakriterie.kriterie.plus(medlemskapOffentligOgInntektKriterie),
        LocalDateTime.now()
    )
}

private fun kriterieMedlemskapOffentligOgInntekt(
    arbeidsforholdOpplysning: Opplysning.Periodisert<AaregResponse?>,
    inntekt: Opplysning.Konstant<InntektsOpplysning>,
    doedsdato: LocalDate,
    saksbehandlerMedlemskapsperioder: VilkaarOpplysning<SaksbehandlerMedlemskapsperioder>?
): Kriterie {
    val inntektsOpplysning = inntekt.let {
        VilkaarOpplysning(
            id = it.id,
            kilde = it.kilde,
            opplysningType = Opplysningstyper.INNTEKT,
            opplysning = it.verdi
        )
    }

    val avdoedesMedlemskapGrunnlag = AvdoedesMedlemskapGrunnlag(
        inntektsOpplysning = inntektsOpplysning,
        arbeidsforholdOpplysning = arbeidsforholdOpplysning,
        saksbehandlerMedlemsPerioder = saksbehandlerMedlemskapsperioder,
        doedsdato = doedsdato
    )

    val ufoere = finnPensjonEllerTrygdePerioder(avdoedesMedlemskapGrunnlag, PeriodeType.UFOERETRYGD, "ufoeretrygd")
    val alder = finnPensjonEllerTrygdePerioder(avdoedesMedlemskapGrunnlag, PeriodeType.ALDERSPENSJON, "alderspensjon")
    val arbeidsforhold = finnArbeidsforholdPerioder(avdoedesMedlemskapGrunnlag)
    val saksbehandlerPerioder = finnSaksbehandlerMedlemsPerioder(avdoedesMedlemskapGrunnlag)
    val inntektsperioder = finnArbeidsinntektPerioder(avdoedesMedlemskapGrunnlag)
    val offentligYtelserPerioder = finnOffentligeYtelserPerioder(avdoedesMedlemskapGrunnlag)

    val perioder: List<VurdertMedlemskapsperiode> =
        ufoere + alder + arbeidsforhold + saksbehandlerPerioder + inntektsperioder + offentligYtelserPerioder

    val opplysning = AvdoedesMedlemskapVurdering(
        grunnlag = avdoedesMedlemskapGrunnlag,
        perioder = perioder,
        gaps = finnGapsIGodkjentePerioder(perioder, doedsdato.minusYears(5), doedsdato)
    )
    val grunnlag = Kriteriegrunnlag(
        id = UUID.randomUUID(),
        kriterieOpplysningsType = KriterieOpplysningsType.AVDOED_MEDLEMSKAP,
        kilde = Grunnlagsopplysning.Vilkaarskomponenten("vilkaarskomponenten"),
        opplysning = opplysning
    )

    return Kriterie(
        navn = Kriterietyper.AVDOED_OPPFYLLER_MEDLEMSKAP,
        resultat = if (opplysning.gaps.isEmpty()) VurderingsResultat.OPPFYLT else VurderingsResultat.IKKE_OPPFYLT,
        basertPaaOpplysninger = listOf(grunnlag)
    )
}

fun finnGapsIGodkjentePerioder(
    perioder: List<VurdertMedlemskapsperiode>,
    fra: LocalDate,
    til: LocalDate
): List<Gap> =
    perioder
        .filter { it.godkjentPeriode }
        .map { Periode(it.fraDato, it.tilDato) }
        .sortedBy { it.gyldigFra }
        .let { hentGaps(kombinerPerioder(it), fra, til) }
        .map { Gap(it.gyldigFra, it.gyldigTil) }