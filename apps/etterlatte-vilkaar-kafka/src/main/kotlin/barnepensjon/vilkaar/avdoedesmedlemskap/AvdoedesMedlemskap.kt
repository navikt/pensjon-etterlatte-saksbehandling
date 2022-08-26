package barnepensjon.vilkaar.avdoedesmedlemskap

import no.nav.etterlatte.libs.common.arbeidsforhold.AaregAnsettelsesdetaljer
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregAnsettelsesperiode
import no.nav.etterlatte.libs.common.arbeidsforhold.ArbeidsforholdOpplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.inntekt.InntektsOpplysning
import no.nav.etterlatte.libs.common.inntekt.UtbetaltPeriode
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
import java.time.LocalDateTime
import java.time.YearMonth

fun vilkaarAvdoedesMedlemskap(
    avdoedSoeknad: VilkaarOpplysning<AvdoedSoeknad>?,
    avdoedPdl: VilkaarOpplysning<Person>?,
    inntektsOpplysning: VilkaarOpplysning<InntektsOpplysning>?,
    arbeidsforholdOpplysning: VilkaarOpplysning<ArbeidsforholdOpplysning>?
): VurdertVilkaar {
    // Kriterier:
    // 1. bodd i norge siste 5 årene
    val bosattNorge = metakriterieBosattNorge(avdoedSoeknad, avdoedPdl).kriterie
    // 2. Arbeidet i norge siste 5 årene
    // Kriterie A2-1 (men skal være 80%)
    val harHatt100prosentStillingSisteFemAar = kritieeHarHatt100prosentStillingSisteFemAar(arbeidsforholdOpplysning)
    // 3. ingen opphold utenfor Norge
    // ELLER :
    val kriterier: List<Kriterie> = listOf(
        // kriterie A1-1: AP eller uføretrygd i hele 5-årsperioden
        kriterieHarMottattPensjonEllerTrygdSisteFemAar(inntektsOpplysning)
    )

    return VurdertVilkaar(
        Vilkaartyper.AVDOEDES_FORUTGAAENDE_MEDLEMSKAP,
        VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, // endre når vi får inn flere opplysninger
        Utfall.TRENGER_AVKLARING, // endre når vi får inn flere opplysninger
        kriterier,
        LocalDateTime.now()
    )
}

data class DetaljerPeriode(
    val periode: AaregAnsettelsesperiode,
    val ansettelsesdetaljer: List<AaregAnsettelsesdetaljer>
)

fun kritieeHarHatt100prosentStillingSisteFemAar(
    arbeidsforholdOpplysning: VilkaarOpplysning<ArbeidsforholdOpplysning>?
): Kriterie {
    val perioder = arrayListOf<DetaljerPeriode>()
    arbeidsforholdOpplysning?.opplysning?.arbeidsforhold?.forEach {
        perioder.add(DetaljerPeriode(it.ansettelsesperiode, it.ansettelsesdetaljer))
    }

    val opplysningsGrunnlag = listOfNotNull(
        arbeidsforholdOpplysning?.let {
            Kriteriegrunnlag(
                arbeidsforholdOpplysning.id,
                KriterieOpplysningsType.AVDOED_STILLINGSPROSENT,
                arbeidsforholdOpplysning.kilde,
                perioder
            )
        }
    )

    return Kriterie(
        Kriterietyper.AVDOED_HAR_HATT_100PROSENT_STILLING_SISTE_FEM_AAR,
        VurderingsResultat.OPPFYLT,
        opplysningsGrunnlag
    )
}

fun kriterieHarMottattPensjonEllerTrygdSisteFemAar(
    inntektsOpplysning: VilkaarOpplysning<InntektsOpplysning>?
): Kriterie {
    val grunnlag = inntektsOpplysning?.opplysning?.pensjonEllerTrygd?.let { inntekter ->
        inntekter.filter { listOf("ufoeretrygd", "alderspensjon").contains(it.beskrivelse) }
            .map { UtbetaltPeriode(YearMonth.parse(it.utbetaltIMaaned), it.beloep) }
            .sortedBy { it.gyldigFraOgMed }
            .let { utbetalingsPerioder ->
                listOf(
                    Kriteriegrunnlag(
                        inntektsOpplysning.id,
                        KriterieOpplysningsType.AVDOED_UFORE_PENSJON,
                        Grunnlagsopplysning.Inntektskomponenten("inntektskomponenten"),
                        utbetalingsPerioder
                    )
                )
            }
    }

    // todo: vurder vilkåret ved å se på periodene.
    return if (grunnlag != null) {
        Kriterie(
            navn = Kriterietyper.AVDOED_HAR_MOTTATT_PENSJON_TRYGD_SISTE_FEM_AAR,
            resultat = VurderingsResultat.OPPFYLT,
            basertPaaOpplysninger = grunnlag
        )
    } else {
        return Kriterie(
            navn = Kriterietyper.AVDOED_HAR_MOTTATT_PENSJON_TRYGD_SISTE_FEM_AAR,
            resultat = VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            basertPaaOpplysninger = emptyList()
        )
    }
}