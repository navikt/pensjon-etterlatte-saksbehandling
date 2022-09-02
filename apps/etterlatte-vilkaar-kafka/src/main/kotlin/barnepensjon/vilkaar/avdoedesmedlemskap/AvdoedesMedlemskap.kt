package barnepensjon.vilkaar.avdoedesmedlemskap

import barnepensjon.vilkaar.avdoedesmedlemskap.perioder.finnArbeidsforholdPerioder
import barnepensjon.vilkaar.avdoedesmedlemskap.perioder.finnPensjonEllerTrygdePerioder
import barnepensjon.vilkaar.avdoedesmedlemskap.perioder.finnSaksbehandlerMedlemsPerioder
import no.nav.etterlatte.barnepensjon.Periode
import no.nav.etterlatte.barnepensjon.hentDoedsdato
import no.nav.etterlatte.barnepensjon.hentGaps
import no.nav.etterlatte.barnepensjon.hentVurdering
import no.nav.etterlatte.barnepensjon.kombinerPerioder
import no.nav.etterlatte.libs.common.arbeidsforhold.ArbeidsforholdOpplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapVurdering
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapsperiode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Gap
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.VurdertMedlemskapsPeriode
import no.nav.etterlatte.libs.common.inntekt.InntektsOpplysning
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.Utfall
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun vilkaarAvdoedesMedlemskap(
    avdoedSoeknad: VilkaarOpplysning<AvdoedSoeknad>?,
    avdoedPdl: VilkaarOpplysning<Person>?,
    inntektsOpplysning: VilkaarOpplysning<InntektsOpplysning>?,
    arbeidsforholdOpplysning: VilkaarOpplysning<ArbeidsforholdOpplysning>?,
    saksbehandlerMedlemskapsPerioder: List<VilkaarOpplysning<AvdoedesMedlemskapsperiode>>?
): VurdertVilkaar {
    if (listOf(avdoedSoeknad, avdoedPdl, inntektsOpplysning, arbeidsforholdOpplysning).any { it == null }) {
        return VurdertVilkaar(
            Vilkaartyper.AVDOEDES_FORUTGAAENDE_MEDLEMSKAP,
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            Utfall.TRENGER_AVKLARING,
            emptyList(),
            LocalDateTime.now()
        )
    }
    val doedsdato = hentDoedsdato(avdoedPdl!!)
    val bosattNorge = metakriterieBosattNorge(avdoedSoeknad, avdoedPdl)

    val avdoedesMedlemskapGrunnlag = AvdoedesMedlemskapGrunnlag(
        inntektsOpplysning = inntektsOpplysning!!,
        arbeidsforholdOpplysning = arbeidsforholdOpplysning!!,
        saksbehandlerMedlemsPerioder = saksbehandlerMedlemskapsPerioder ?: emptyList(),
        doedsdato = doedsdato,
        bosattNorge = bosattNorge
    )

    val ufoere = finnPensjonEllerTrygdePerioder(avdoedesMedlemskapGrunnlag, PeriodeType.UFØRETRYGD, "ufoeretrygd")
    val alder = finnPensjonEllerTrygdePerioder(avdoedesMedlemskapGrunnlag, PeriodeType.ALDERSPENSJON, "alderspensjon")
    val arbeidsforhold = finnArbeidsforholdPerioder(avdoedesMedlemskapGrunnlag)
    val saksbehandlerPerioder = finnSaksbehandlerMedlemsPerioder(avdoedesMedlemskapGrunnlag)

    val perioder: List<VurdertMedlemskapsPeriode> = ufoere + alder + arbeidsforhold + saksbehandlerPerioder

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
    val vurderingsResultat = hentVurdering(listOf(kriterie.resultat, bosattNorge.resultat))

    return VurdertVilkaar(
        Vilkaartyper.AVDOEDES_FORUTGAAENDE_MEDLEMSKAP,
        vurderingsResultat,
        bosattNorge.utfall,
        listOf(kriterie),
        LocalDateTime.now()
    )
}

fun finnGapsIGodkjentePerioder(
    perioder: List<VurdertMedlemskapsPeriode>,
    fra: LocalDate,
    til: LocalDate
): List<Gap> =
    perioder
        .filter { it.godkjentPeriode }
        .map { Periode(it.fraDato, it.tilDato ?: til) }
        .sortedBy { it.gyldigFra }
        .let { hentGaps(kombinerPerioder(it), fra, til) }
        .map { Gap(it.gyldigFra, it.gyldigTil) }