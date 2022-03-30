package no.nav.etterlatte.barnepensjon

import no.nav.etterlatte.libs.common.behandling.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VilkaarVurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import java.time.LocalDateTime

fun vilkaarAvdoedesMedlemskap(
    vilkaartype: Vilkaartyper,
    avdoedSoeknad: VilkaarOpplysning<AvdoedSoeknad>?,
    avdoedPdl: VilkaarOpplysning<Person>?,
): VurdertVilkaar {

    val utenlandsoppholdSisteFemAarene = kriterieIngenUtenlandsoppholdSisteFemAar(avdoedSoeknad, avdoedPdl)
    // Kriterier: 1. bodd i norge siste 5 årene
    // 2. Arbeidet i norge siste 5 årene
    // 3. opphold utenfor Norge
    // ELLER :
    // 4. mottatt trydg / uføre eller pensjon siste 5 årene

    return VurdertVilkaar(
        vilkaartype,
        VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, //endre når vi får inn flere opplysninger
        listOf(utenlandsoppholdSisteFemAarene),
        LocalDateTime.now()
    )
}

//TODO: dette vil nok utgå om vi henter inn medlemskap fra LovMe istedet.

fun kriterieIngenUtenlandsoppholdSisteFemAar(
    avdoedSoeknad: VilkaarOpplysning<AvdoedSoeknad>?,
    avdoedPdl: VilkaarOpplysning<Person>?,
): Kriterie {

    val opplysningsGrunnlag = listOfNotNull(
        avdoedPdl?.let {
            Kriteriegrunnlag(
                KriterieOpplysningsType.DOEDSDATO,
                avdoedPdl.kilde,
                Doedsdato(avdoedPdl.opplysning.doedsdato, avdoedPdl.opplysning.foedselsnummer)
            )
        },
        avdoedSoeknad?.let {
            Kriteriegrunnlag(
                KriterieOpplysningsType.AVDOED_UTENLANDSOPPHOLD,
                avdoedSoeknad.kilde,
                avdoedSoeknad.opplysning.utenlandsopphold
            )
        }
    )

    if (avdoedPdl == null || avdoedSoeknad == null) return opplysningsGrunnlagNull(
        Kriterietyper.AVDOED_IKKE_OPPHOLD_UTLAND_SISTE_FEM_AAR,
        opplysningsGrunnlag
    )

    val ingenOppholdUtlandetSisteFemAar = try {
        val femAarFoerDoedsdato = hentDoedsdato(avdoedPdl).minusYears(5)
        val utenlandsoppholdSoeknad = avdoedSoeknad.opplysning.utenlandsopphold
        val oppholdSisteFemAAr = utenlandsoppholdSoeknad.opphold?.map { it.tilDato?.isAfter(femAarFoerDoedsdato) }

        if (oppholdSisteFemAAr != null && oppholdSisteFemAAr.contains(true)) {
            VilkaarVurderingsResultat.IKKE_OPPFYLT
        } else {
            VilkaarVurderingsResultat.OPPFYLT
        }

    } catch (ex: OpplysningKanIkkeHentesUt) {
        VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    }

    return Kriterie(
        Kriterietyper.AVDOED_IKKE_OPPHOLD_UTLAND_SISTE_FEM_AAR, ingenOppholdUtlandetSisteFemAar, opplysningsGrunnlag
    )

}
