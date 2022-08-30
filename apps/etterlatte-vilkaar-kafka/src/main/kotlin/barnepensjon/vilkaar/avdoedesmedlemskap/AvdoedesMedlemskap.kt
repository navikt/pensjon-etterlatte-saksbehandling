package barnepensjon.vilkaar.avdoedesmedlemskap

import no.nav.etterlatte.barnepensjon.Periode
import no.nav.etterlatte.barnepensjon.hentDoedsdato
import no.nav.etterlatte.barnepensjon.hentGaps
import no.nav.etterlatte.barnepensjon.kombinerPerioder
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregAnsettelsesdetaljer
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregAnsettelsesperiode
import no.nav.etterlatte.libs.common.arbeidsforhold.ArbeidsforholdOpplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.inntekt.Inntekt
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
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters.firstDayOfNextMonth

fun vilkaarAvdoedesMedlemskap(
    avdoedSoeknad: VilkaarOpplysning<AvdoedSoeknad>?,
    avdoedPdl: VilkaarOpplysning<Person>?,
    inntektsOpplysning: VilkaarOpplysning<InntektsOpplysning>?,
    arbeidsforholdOpplysning: VilkaarOpplysning<ArbeidsforholdOpplysning>?
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

    val kriterier: List<Kriterie> = listOfNotNull(
        // kriterie A1-1: AP eller uføretrygd i hele 5-årsperioden
        kriterieHarMottattPensjonEllerTrygdSisteFemAar(doedsdato, inntektsOpplysning!!),
        // Kriterie A2-1: Arbeidd 80% (stilling) hele 5-årsperioden
        kriterieHarHatt80prosentStillingSisteFemAar(doedsdato, arbeidsforholdOpplysning!!, inntektsOpplysning)
    )

    val vurderingsResultat = when (bosattNorge.utfall) {
        Utfall.OPPFYLT -> kriterier.minBy { it.resultat.prioritet }.resultat
        else -> bosattNorge.resultat
    }

    return VurdertVilkaar(
        Vilkaartyper.AVDOEDES_FORUTGAAENDE_MEDLEMSKAP,
        vurderingsResultat,
        vurderingsResultat.tilUtfall(),
        kriterier,
        LocalDateTime.now()
    )
}

private fun VurderingsResultat.tilUtfall() = when (this) {
    VurderingsResultat.OPPFYLT -> Utfall.OPPFYLT
    VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING -> Utfall.TRENGER_AVKLARING
    VurderingsResultat.IKKE_OPPFYLT -> Utfall.BEHANDLE_I_PSYS
}

data class DetaljerPeriode(
    val periode: AaregAnsettelsesperiode,
    val ansettelsesdetaljer: List<AaregAnsettelsesdetaljer>
)

fun kriterieHarHatt80prosentStillingSisteFemAar(
    doedsdato: LocalDate,
    arbeidsforholdOpplysning: VilkaarOpplysning<ArbeidsforholdOpplysning>,
    inntektsOpplysning: VilkaarOpplysning<InntektsOpplysning>
): Kriterie {
    val perioder = arbeidsforholdOpplysning.opplysning.arbeidsforhold
        .filter { forhold -> forhold.ansettelsesdetaljer.any { it.avtaltStillingsprosent >= 80 } }
        .map { DetaljerPeriode(it.ansettelsesperiode, it.ansettelsesdetaljer) }
    val inntekt = inntektsOpplysning.opplysning.loennsinntekt.plus(inntektsOpplysning.opplysning.naeringsinntekt)
        .filter { it.beloep > 0 }
    val ansettelsesGaps = finnGapsIArbeidsforhold(perioder, doedsdato.minusYears(5), doedsdato)

    val grunnlag = Kriteriegrunnlag(
        id = arbeidsforholdOpplysning.id,
        kriterieOpplysningsType = KriterieOpplysningsType.AVDOED_STILLINGSPROSENT,
        kilde = arbeidsforholdOpplysning.kilde,
        opplysning = MedlemskapsPeriode(
            inntekter = inntekt,
            gaps = ansettelsesGaps,
            arbeidsforhold = perioder
        )
    )

    return Kriterie(
        navn = Kriterietyper.AVDOED_HAR_HATT_100PROSENT_STILLING_SISTE_FEM_AAR,
        resultat = if (ansettelsesGaps.isEmpty()) VurderingsResultat.OPPFYLT else VurderingsResultat.IKKE_OPPFYLT,
        basertPaaOpplysninger = listOf(grunnlag)
    )
}

fun kriterieHarMottattPensjonEllerTrygdSisteFemAar(
    doedsdato: LocalDate,
    inntektsOpplysning: VilkaarOpplysning<InntektsOpplysning>
): Kriterie {
    val inntekter = inntektsOpplysning.opplysning.pensjonEllerTrygd
        .filter { listOf("ufoeretrygd", "alderspensjon").contains(it.beskrivelse) }
    val inntektsGaps = finnGapsIUtbetalinger(inntekter, doedsdato.minusYears(5), doedsdato)

    val grunnlag = Kriteriegrunnlag(
        id = inntektsOpplysning.id,
        kriterieOpplysningsType = KriterieOpplysningsType.AVDOED_UFORE_PENSJON,
        kilde = Grunnlagsopplysning.Inntektskomponenten("inntektskomponenten"),
        opplysning = MedlemskapsPeriode(
            inntekter = inntekter,
            gaps = inntektsGaps
        )
    )

    return Kriterie(
        navn = Kriterietyper.AVDOED_HAR_MOTTATT_PENSJON_TRYGD_SISTE_FEM_AAR,
        resultat = if (inntektsGaps.isEmpty()) VurderingsResultat.OPPFYLT else VurderingsResultat.IKKE_OPPFYLT,
        basertPaaOpplysninger = listOf(grunnlag)
    )
}

fun finnGapsIUtbetalinger(utbetalinger: List<Inntekt>, fra: LocalDate, til: LocalDate): List<Periode> =
    utbetalinger.map {
        val utbetaltIMaaned = YearMonth.parse(it.utbetaltIMaaned)
        val gyldigFra = LocalDate.of(utbetaltIMaaned.year, utbetaltIMaaned.month, 1)

        Periode(gyldigFra, gyldigFra.with(firstDayOfNextMonth()))
    }.sortedBy { it.gyldigFra }.let { hentGaps(kombinerPerioder(it), fra, til) }

fun finnGapsIArbeidsforhold(arbeidsforhold: List<DetaljerPeriode>, fra: LocalDate, til: LocalDate) =
    arbeidsforhold.map { Periode(it.periode.startdato, it.periode.sluttdato ?: til) }
        .sortedBy { it.gyldigFra }
        .let { hentGaps(kombinerPerioder(it), fra, til) }

data class MedlemskapsPeriode(
    val inntekter: List<Inntekt>,
    val gyldigePerioder: List<Periode>? = null,
    val gaps: List<Periode>? = null,
    val arbeidsforhold: List<DetaljerPeriode>? = null
)