package barnepensjon.vilkaar.avdoedesmedlemskap

import no.nav.etterlatte.barnepensjon.opplysningsGrunnlagNull
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregAnsettelsesdetaljer
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregAnsettelsesperiode
import no.nav.etterlatte.libs.common.arbeidsforhold.ArbeidsforholdOpplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.inntekt.PensjonUforeOpplysning
import no.nav.etterlatte.libs.common.inntekt.UtbetaltPeriode
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList


fun vilkaarAvdoedesMedlemskap(
    vilkaartype: Vilkaartyper,
    avdoedSoeknad: VilkaarOpplysning<AvdoedSoeknad>?,
    avdoedPdl: VilkaarOpplysning<Person>?,
    pensjonUforeOpplysning: VilkaarOpplysning<PensjonUforeOpplysning>?,
    arbeidsforholdOpplysning: VilkaarOpplysning<ArbeidsforholdOpplysning>?
): VurdertVilkaar {
    // Kriterier: 1. bodd i norge siste 5 årene
    // 2. Arbeidet i norge siste 5 årene
    // 3. ingen opphold utenfor Norge
    // ELLER :
    // 4. mottatt trydg / uføre eller pensjon siste 5 årene

    val bosattNorge = metakriterieBosattNorge(avdoedSoeknad, avdoedPdl)
    // 4 outcomes: 1. oppfylt, 2. ikke oppfylt, 3. send til psys, 4. trenger avklaring/manuelt input, 5? kan ikke vurdere

    // fjernes
    val harMottattUforeTrygdSisteFemAar =
        kriterieHarMottattUforeTrygdSisteFemAar(pensjonUforeOpplysning)
    val harMottattPensjonSisteFemAar =
        kriterieHarMottattPensjonSisteFemAar(pensjonUforeOpplysning)

    val harMottattPensjonEllerTrygdSisteFemAar = kriterieHarMottattPensjonEllerTrygdSisteFemAar(pensjonUforeOpplysning)


    val harHatt100prosentStillingSisteFemAar = kritieeHarHatt100prosentStillingSisteFemAar(arbeidsforholdOpplysning)

    return VurdertVilkaar(
        vilkaartype,
        VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, //endre når vi får inn flere opplysninger
        null,
        listOf(
           // ingenUtenlandsoppholdOppgittISoeknad,
           // sammenhengendeAdresserINorgeSisteFemAar,
            harMottattUforeTrygdSisteFemAar,
            harMottattPensjonSisteFemAar,
            harHatt100prosentStillingSisteFemAar
        ),
        LocalDateTime.now()
    )
}

data class DetaljerPeriode(
    val periode: AaregAnsettelsesperiode,
    val ansettelsesdetaljer: List<AaregAnsettelsesdetaljer>
)

fun kritieeHarHatt100prosentStillingSisteFemAar(arbeidsforholdOpplysning: VilkaarOpplysning<ArbeidsforholdOpplysning>?): Kriterie {

    val perioder = arrayListOf<DetaljerPeriode>()
    arbeidsforholdOpplysning?.opplysning?.arbeidsforhold?.forEach {
        perioder.add(DetaljerPeriode(it.ansettelsesperiode, it.ansettelsesdetaljer))
    }

    val opplysningsGrunnlag = listOfNotNull(arbeidsforholdOpplysning?.let {
        Kriteriegrunnlag(
            arbeidsforholdOpplysning.id,
            KriterieOpplysningsType.AVDOED_STILLINGSPROSENT,
            arbeidsforholdOpplysning.kilde,
            perioder
        )
    })

    return Kriterie(
        Kriterietyper.AVDOED_HAR_HATT_100PROSENT_STILLING_SISTE_FEM_AAR,
        VurderingsResultat.OPPFYLT,
        opplysningsGrunnlag
    )
}



fun kriterieHarMottattPensjonEllerTrygdSisteFemAar(pensjonUforeOpplysning: VilkaarOpplysning<PensjonUforeOpplysning>?): Kriterie {
    val inntekter = ArrayList<UtbetaltPeriode>()


    pensjonUforeOpplysning?.opplysning?.mottattUforetrygd?.forEach {
        inntekter.add(it)
    }
    pensjonUforeOpplysning?.opplysning?.mottattAlderspensjon?.forEach {
        inntekter.add(it)
    }

    inntekter.sortBy { utbetaltPeriode ->
        utbetaltPeriode.gyldigFraOgMed
    }

    val grunnlag = listOfNotNull(
        pensjonUforeOpplysning?.let { it ->
            Kriteriegrunnlag(
                it.id,
                KriterieOpplysningsType.AVDOED_UFORE_PENSJON,
                Grunnlagsopplysning.Inntektskomponenten("inntektskomponenten"),
                inntekter
            )
        }
    )


    val alderspensjon = listOfNotNull(
        pensjonUforeOpplysning?.opplysning?.mottattAlderspensjon?.let {
            Kriteriegrunnlag(
                pensjonUforeOpplysning.id,
                KriterieOpplysningsType.AVDOED_UFORE_PENSJON,
                Grunnlagsopplysning.Inntektskomponenten("inntektskomponenten"),
                pensjonUforeOpplysning.opplysning.mottattAlderspensjon
            )
        }
    )
/*
val uforetrygd = listOfNotNull(
    pensjonUforeOpplysning?.opplysning?.mottattUforetrygd?.let {
        Kriteriegrunnlag(
            pensjonUforeOpplysning.id,
            KriterieOpplysningsType.AVDOED_UFORE_PENSJON,
            Grunnlagsopplysning.Inntektskomponenten("inntektskomponenten"),
            pensjonUforeOpplysning.opplysning.mottattUforetrygd
        )
    }
)
*/

    return Kriterie(
        Kriterietyper.AVDOED_HAR_MOTTATT_PENSJON_TRYGD_SISTE_FEM_AAR,
        VurderingsResultat.OPPFYLT,
        grunnlag
    )
}

fun kriterieHarMottattPensjonSisteFemAar(pensjonUforeOpplysning: VilkaarOpplysning<PensjonUforeOpplysning>?): Kriterie {
    val opplysningsGrunnlag = listOfNotNull(
        pensjonUforeOpplysning?.opplysning?.mottattAlderspensjon?.let {
            Kriteriegrunnlag(
                pensjonUforeOpplysning.id,
                KriterieOpplysningsType.AVDOED_UFORE_PENSJON,
                Grunnlagsopplysning.Inntektskomponenten("inntektskomponenten"),
                pensjonUforeOpplysning.opplysning.mottattAlderspensjon
            )
        }
    )

    if (pensjonUforeOpplysning?.opplysning?.mottattAlderspensjon == null) return opplysningsGrunnlagNull(
        Kriterietyper.AVDOED_HAR_MOTTATT_PENSJON_SISTE_FEM_AAR,
        opplysningsGrunnlag
    )

    pensjonUforeOpplysning.opplysning.mottattUforetrygd
    val resultat = VurderingsResultat.OPPFYLT //TODO logikk her

    return Kriterie(
        Kriterietyper.AVDOED_HAR_MOTTATT_PENSJON_SISTE_FEM_AAR,
        resultat,
        opplysningsGrunnlag
    )
}


fun kriterieHarMottattUforeTrygdSisteFemAar(pensjonUforeOpplysning: VilkaarOpplysning<PensjonUforeOpplysning>?): Kriterie {

    val opplysningsGrunnlag = listOfNotNull(
        pensjonUforeOpplysning?.opplysning?.mottattUforetrygd?.let {
            print("uforetrygd")
            print(pensjonUforeOpplysning.opplysning.mottattUforetrygd)
            Kriteriegrunnlag(
                pensjonUforeOpplysning.id,
                KriterieOpplysningsType.AVDOED_UFORE_PENSJON,
                Grunnlagsopplysning.Inntektskomponenten("inntektskomponenten"),
                pensjonUforeOpplysning.opplysning.mottattUforetrygd
            )
        }
    )

    if (pensjonUforeOpplysning?.opplysning?.mottattUforetrygd == null) return opplysningsGrunnlagNull(
        Kriterietyper.AVDOED_HAR_MOTTATT_TRYGD_SISTE_FEM_AAR,
        opplysningsGrunnlag
    )

    val resultat = VurderingsResultat.OPPFYLT //TODO logikk her

    return Kriterie(
        Kriterietyper.AVDOED_HAR_MOTTATT_TRYGD_SISTE_FEM_AAR,
        resultat,
        opplysningsGrunnlag
    )
}