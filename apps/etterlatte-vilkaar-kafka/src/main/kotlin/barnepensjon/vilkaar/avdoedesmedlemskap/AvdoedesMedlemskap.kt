package barnepensjon.vilkaar.avdoedesmedlemskap

import barnepensjon.vilkaar.avdoedesmedlemskap.perioder.finnArbeidsforholdPerioder
import barnepensjon.vilkaar.avdoedesmedlemskap.perioder.finnArbeidsinntektPerioder
import barnepensjon.vilkaar.avdoedesmedlemskap.perioder.finnOffentligeYtelserPerioder
import barnepensjon.vilkaar.avdoedesmedlemskap.perioder.finnPensjonEllerTrygdePerioder
import barnepensjon.vilkaar.avdoedesmedlemskap.perioder.finnSaksbehandlerMedlemsPerioder
import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.barnepensjon.OpplysningKanIkkeHentesUt
import no.nav.etterlatte.barnepensjon.Periode
import no.nav.etterlatte.barnepensjon.hentGaps
import no.nav.etterlatte.barnepensjon.hentVurdering
import no.nav.etterlatte.barnepensjon.kombinerPerioder
import no.nav.etterlatte.libs.common.arbeidsforhold.ArbeidsforholdOpplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapVurdering
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Gap
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
    avdoedSoeknad: VilkaarOpplysning<AvdoedSoeknad>?,
    avdoedPdl: Grunnlagsdata<JsonNode>,
    inntektsOpplysning: VilkaarOpplysning<InntektsOpplysning>?,
    arbeidsforholdOpplysning: VilkaarOpplysning<ArbeidsforholdOpplysning>?,
    saksbehandlerMedlemskapsPerioder: VilkaarOpplysning<SaksbehandlerMedlemskapsperioder>?
): VurdertVilkaar {
    if (listOf(avdoedSoeknad, avdoedPdl, inntektsOpplysning, arbeidsforholdOpplysning).any { it == null }) {
        return VurdertVilkaar(
            Vilkaartyper.AVDOEDES_FORUTGAAENDE_MEDLEMSKAP,
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            null,
            emptyList(),
            LocalDateTime.now()
        )
    }

    val bosattNorgeMetakriterie = metakriterieBosattNorge(avdoedSoeknad, avdoedPdl)

    val medlemskapOffentligOgInntektKriterie = kriterieMedlemskapOffentligOgInntekt(
        avdoedPdl,
        inntektsOpplysning!!,
        arbeidsforholdOpplysning!!,
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

fun kriterieMedlemskapOffentligOgInntekt(
    avdoedPdl: Grunnlagsdata<JsonNode>,
    inntektsOpplysning: VilkaarOpplysning<InntektsOpplysning>,
    arbeidsforholdOpplysning: VilkaarOpplysning<ArbeidsforholdOpplysning>,
    saksbehandlerMedlemskapsperioder: VilkaarOpplysning<SaksbehandlerMedlemskapsperioder>?
): Kriterie {
    val doedsdato = avdoedPdl.hentDoedsdato()?.verdi ?: throw OpplysningKanIkkeHentesUt()

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
    val kriterie = Kriterie(
        navn = Kriterietyper.AVDOED_OPPFYLLER_MEDLEMSKAP,
        resultat = if (opplysning.gaps.isEmpty()) VurderingsResultat.OPPFYLT else VurderingsResultat.IKKE_OPPFYLT,
        basertPaaOpplysninger = listOf(grunnlag)
    )

    return kriterie
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