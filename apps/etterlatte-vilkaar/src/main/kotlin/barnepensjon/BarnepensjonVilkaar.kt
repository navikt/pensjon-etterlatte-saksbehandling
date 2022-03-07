package no.nav.etterlatte.vilkaar.barnepensjon

import no.nav.etterlatte.barnepensjon.OpplysningKanIkkeHentesUt
import no.nav.etterlatte.barnepensjon.hentBostedsadresse
import no.nav.etterlatte.barnepensjon.hentDoedsdato
import no.nav.etterlatte.barnepensjon.hentFnrForeldre
import no.nav.etterlatte.barnepensjon.hentOppholdadresse
import no.nav.etterlatte.barnepensjon.hentSoekerFoedselsdato
import no.nav.etterlatte.barnepensjon.hentUtenlandsadresseSoeknad
import no.nav.etterlatte.barnepensjon.hentUtenlandsopphold
import no.nav.etterlatte.barnepensjon.hentVirkningsdato
import no.nav.etterlatte.barnepensjon.setVikaarVurderingsResultat
import no.nav.etterlatte.barnepensjon.vurderOpplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Adresse
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Bostedadresse
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foedselsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foreldre
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Oppholdadresse
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Utenlandsadresse
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Utenlandsopphold
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VilkaarVurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import java.time.LocalDate

fun vilkaarBrukerErUnder20(
    vilkaartype: Vilkaartyper,
    foedselsdato: List<VilkaarOpplysning<Foedselsdato>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>,
): VurdertVilkaar {
    val soekerErUnder20 = kriterieSoekerErUnder20(foedselsdato, doedsdato)

    return VurdertVilkaar(
        vilkaartype,
        setVikaarVurderingsResultat(listOf(soekerErUnder20)),
        listOf(soekerErUnder20)
    )
}

fun vilkaarDoedsfallErRegistrert(
    vilkaartype: Vilkaartyper,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>,
    foreldre: List<VilkaarOpplysning<Foreldre>>,
): VurdertVilkaar {
    val doedsdatoRegistrertIPdl = kriterieDoedsdatoRegistrertIPdl(doedsdato)
    val avdoedErForeldre = kriterieAvdoedErForelder(foreldre, doedsdato)

    return VurdertVilkaar(
        vilkaartype,
        setVikaarVurderingsResultat(listOf(doedsdatoRegistrertIPdl, avdoedErForeldre)),
        listOf(doedsdatoRegistrertIPdl, avdoedErForeldre)
    )
}

fun vilkaarAvdoedesMedlemskap(
    vilkaartype: Vilkaartyper,
    avdoedUtenlandsopphold: List<VilkaarOpplysning<Utenlandsopphold>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>,
): VurdertVilkaar {

    val utenlandsoppholdSisteFemAarene = kriterieIngenUtenlandsopphold(avdoedUtenlandsopphold, doedsdato)
    // Kriterier: 1. bodd i norge siste 5 årene
    // 2. Arbeidet i norge siste 5 årene
    // 3. opphold utenfor Norge
    // ELLER :
    // 4. mottatt trydg / uføre eller pensjon siste 5 årene

    return VurdertVilkaar(
        vilkaartype,
        VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, //endre når vi får inn flere opplysninger
        listOf(utenlandsoppholdSisteFemAarene)
    )
}


fun vilkaarBarnetsMedlemskap(
    vilkaartype: Vilkaartyper,
    soekerBostedadresse: List<VilkaarOpplysning<Bostedadresse>>,
    soekerOppholdadresse: List<VilkaarOpplysning<Oppholdadresse>>,
    soekerUtenlandsadresse: List<VilkaarOpplysning<Utenlandsadresse>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>,
): VurdertVilkaar {
    val harIkkeBostedadresseIUtlandetPdl =
        kriterieHarIkkeBostedsadresseIUtlandet(soekerBostedadresse, doedsdato)
    val harIkkeOppholdsadresseIUtlandetPdl =
        kriterieHarIkkeOppholddsadresseIUtlandet(soekerOppholdadresse, doedsdato)
    val harIkkeBostedadresseIUtlandetSoeknad = kriterieHarIkkeOppgittAdresseIUtlandet(soekerUtenlandsadresse)

    return VurdertVilkaar(
        vilkaartype,
        VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
        listOf(
            harIkkeBostedadresseIUtlandetPdl,
            harIkkeOppholdsadresseIUtlandetPdl,
            harIkkeBostedadresseIUtlandetSoeknad
        )
    )
}

fun kriterieHarIkkeOppgittAdresseIUtlandet(utenlandsadresse: List<VilkaarOpplysning<Utenlandsadresse>>): Kriterie {
    val opplysningsGrunnlag = listOf(utenlandsadresse.filter { it.kilde.type == "privatperson" }).flatten()
    val resultat =
        vurderOpplysning { hentUtenlandsadresseSoeknad(utenlandsadresse).adresseIUtlandet == "NEI" }

    return Kriterie(
        Kriterietyper.SOEKER_IKKE_OPPGITT_ADRESSE_I_UTLANDET_I_SOEKNAD,
        resultat,
        opplysningsGrunnlag
    )
}

fun kriterieHarIkkeBostedsadresseIUtlandet(
    bostedadresse: List<VilkaarOpplysning<Bostedadresse>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>
): Kriterie {
    val opplysningsGrunnlag = listOf(bostedadresse, doedsdato).flatten().filter { it.kilde.type == "pdl" }

    val resultat = try {
        val bostedadresser = hentBostedsadresse(bostedadresse)
        val doedsdato = hentDoedsdato(doedsdato).doedsdato
        harKunNorskeAdresserEtterDoedsdato(bostedadresser.bostedadresse, doedsdato)

    } catch (ex: OpplysningKanIkkeHentesUt) {
        VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    }

    return Kriterie(
        Kriterietyper.SOEKER_IKKE_BOSTEDADRESSE_I_UTLANDET,
        resultat,
        opplysningsGrunnlag
    )
}

fun kriterieHarIkkeOppholddsadresseIUtlandet(
    oppholdadresse: List<VilkaarOpplysning<Oppholdadresse>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>
): Kriterie {
    val opplysningsGrunnlag = listOf(oppholdadresse, doedsdato).flatten().filter { it.kilde.type == "pdl" }
    val resultat = try {
        val oppholdadresse = hentOppholdadresse(oppholdadresse)
        val doedsdato = hentDoedsdato(doedsdato).doedsdato
        harKunNorskeAdresserEtterDoedsdato(oppholdadresse.oppholdadresse, doedsdato)

    } catch (ex: OpplysningKanIkkeHentesUt) {
        VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    }

    return Kriterie(
        Kriterietyper.SOEKER_IKKE_OPPHOLDADRESSE_I_UTLANDET,
        resultat,
        opplysningsGrunnlag
    )
}

fun harKunNorskeAdresserEtterDoedsdato(adresser: List<Adresse>?, doedsdato: LocalDate): VilkaarVurderingsResultat {
    val harUtenlandskeAdresserIPdl = adresser?.filter {
        it.gyldigTilOgMed?.isAfter(doedsdato) == true || it.aktiv
    }?.map { it.land?.lowercase() == "norge" || it.land?.lowercase() == "nor" }?.contains(false)

    return if (harUtenlandskeAdresserIPdl == null) {
        VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    } else if (harUtenlandskeAdresserIPdl == false) {
        VilkaarVurderingsResultat.OPPFYLT
    } else {
        VilkaarVurderingsResultat.IKKE_OPPFYLT
    }
}

fun kriterieIngenUtenlandsopphold(
    utenlandsopphold: List<VilkaarOpplysning<Utenlandsopphold>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>
): Kriterie {

    val ingenOppholdUtlandetSisteFemAar = try {
        val femAarFoerDoedsdato = hentDoedsdato(doedsdato).doedsdato.minusYears(5)
        val utenlandsoppholdSoeknad = hentUtenlandsopphold(utenlandsopphold, "privatperson")
        val oppholdSisteFemAAr = utenlandsoppholdSoeknad.opphold?.map { it.tilDato?.isAfter(femAarFoerDoedsdato) }

        if (oppholdSisteFemAAr != null && oppholdSisteFemAAr.contains(true)) {
            VilkaarVurderingsResultat.IKKE_OPPFYLT
        } else {
            VilkaarVurderingsResultat.OPPFYLT
        }

    } catch (ex: OpplysningKanIkkeHentesUt) {
        VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    }

    val opplysningsGrunnlag = listOf(utenlandsopphold.filter { it.kilde.type == "privatperson" },
        doedsdato.filter { it.kilde.type == "pdl" }).flatten()
    return Kriterie(
        Kriterietyper.AVDOED_IKKE_OPPHOLD_UTLAND_SISTE_FEM_AAR, ingenOppholdUtlandetSisteFemAar, opplysningsGrunnlag
    )

}


fun kriterieSoekerErUnder20(
    foedselsdato: List<VilkaarOpplysning<Foedselsdato>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>
): Kriterie {
    val opplysningsGrunnlag = listOf(foedselsdato, doedsdato).flatten().filter { it.kilde.type == "pdl" }
    val resultat =
        vurderOpplysning { hentSoekerFoedselsdato(foedselsdato).plusYears(20) > hentVirkningsdato(doedsdato) }
    return Kriterie(Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO, resultat, opplysningsGrunnlag)
}

fun kriterieDoedsdatoRegistrertIPdl(doedsdato: List<VilkaarOpplysning<Doedsdato>>): Kriterie {
    val resultat = try {
        hentDoedsdato(doedsdato)
        VilkaarVurderingsResultat.OPPFYLT
    } catch (ex: OpplysningKanIkkeHentesUt) {
        VilkaarVurderingsResultat.IKKE_OPPFYLT
    }

    return Kriterie(
        Kriterietyper.DOEDSFALL_ER_REGISTRERT_I_PDL,
        resultat,
        listOf(doedsdato).flatten().filter { it.kilde.type == "pdl" })
}

fun kriterieAvdoedErForelder(
    foreldre: List<VilkaarOpplysning<Foreldre>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>
): Kriterie {
    val opplsyningsGrunnlag = listOf(foreldre, doedsdato).flatten().filter { it.kilde.type == "pdl" }
    val resultat = vurderOpplysning { hentFnrForeldre(foreldre).contains(hentDoedsdato(doedsdato).foedselsnummer) }

    return Kriterie(Kriterietyper.AVDOED_ER_FORELDER, resultat, opplsyningsGrunnlag)
}






