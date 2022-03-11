package no.nav.etterlatte.vilkaar.barnepensjon

import no.nav.etterlatte.barnepensjon.OpplysningKanIkkeHentesUt
import no.nav.etterlatte.barnepensjon.hentBostedsadresse
import no.nav.etterlatte.barnepensjon.hentDoedsdato
import no.nav.etterlatte.barnepensjon.hentKontaktadresse
import no.nav.etterlatte.barnepensjon.hentOppholdadresse
import no.nav.etterlatte.barnepensjon.hentUtenlandsadresseSoeknad
import no.nav.etterlatte.barnepensjon.setVikaarVurderingsResultat
import no.nav.etterlatte.barnepensjon.vurderOpplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Adresse
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Bostedadresse
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Kontaktadresse
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Oppholdadresse
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.UtenlandsadresseBarn
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VilkaarVurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import java.time.LocalDate

fun vilkaarBarnetsMedlemskap(
    vilkaartype: Vilkaartyper,
    soekerBostedadresse: List<VilkaarOpplysning<Bostedadresse>>,
    soekerOppholdadresse: List<VilkaarOpplysning<Oppholdadresse>>,
    soekerKontaktadresse: List<VilkaarOpplysning<Kontaktadresse>>,
    soekerUtenlandsadresseBarn: List<VilkaarOpplysning<UtenlandsadresseBarn>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>,
): VurdertVilkaar {
    val harIkkeBostedadresseIUtlandetPdl =
        kriterieHarIkkeBostedsadresseIUtlandet(soekerBostedadresse, doedsdato)
    val harIkkeOppholdsadresseIUtlandetPdl =
        kriterieHarIkkeOppholddsadresseIUtlandet(soekerOppholdadresse, doedsdato)
    val harIkkeKontaktadresseIUtlandetPdl =
        kriterieHarIkkeKontaktsadresseIUtlandet(soekerKontaktadresse, doedsdato)
    val harIkkeBostedadresseIUtlandetSoeknad = kriterieHarIkkeOppgittAdresseIUtlandet(soekerUtenlandsadresseBarn)

    return VurdertVilkaar(
        vilkaartype,
        setVikaarVurderingsResultat(listOf(harIkkeBostedadresseIUtlandetPdl, harIkkeBostedadresseIUtlandetSoeknad)),
        listOf(
            harIkkeBostedadresseIUtlandetPdl,
            harIkkeOppholdsadresseIUtlandetPdl,
            harIkkeKontaktadresseIUtlandetPdl,
            harIkkeBostedadresseIUtlandetSoeknad
        )
    )
}

fun kriterieHarIkkeOppgittAdresseIUtlandet(utenlandsadresseBarn: List<VilkaarOpplysning<UtenlandsadresseBarn>>): Kriterie {
    val opplysningsGrunnlag = listOf(utenlandsadresseBarn.filter { it.kilde.type == "privatperson" }).flatten()
    val resultat =
        vurderOpplysning { hentUtenlandsadresseSoeknad(utenlandsadresseBarn).adresseIUtlandet?.lowercase() == "nei" }

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
        val bostedadressePdl = hentBostedsadresse(bostedadresse)
        val doedsdatoPdl = hentDoedsdato(doedsdato).doedsdato
        harKunNorskeAdresserEtterDoedsdato(bostedadressePdl.bostedadresse, doedsdatoPdl)
    } catch (ex: OpplysningKanIkkeHentesUt) {
        VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    }

    return Kriterie(Kriterietyper.SOEKER_IKKE_BOSTEDADRESSE_I_UTLANDET, resultat, opplysningsGrunnlag)
}

fun kriterieHarIkkeOppholddsadresseIUtlandet(
    oppholdadresser: List<VilkaarOpplysning<Oppholdadresse>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>
): Kriterie {
    val opplysningsGrunnlag = listOf(oppholdadresser, doedsdato).flatten().filter { it.kilde.type == "pdl" }
    val resultat = try {
        val oppholdadresser = hentOppholdadresse(oppholdadresser)
        val doedsdatoPdl = hentDoedsdato(doedsdato).doedsdato
        harKunNorskeAdresserEtterDoedsdato(oppholdadresser.oppholdadresse, doedsdatoPdl)
    } catch (ex: OpplysningKanIkkeHentesUt) {
        VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    }

    return Kriterie(Kriterietyper.SOEKER_IKKE_OPPHOLDADRESSE_I_UTLANDET, resultat, opplysningsGrunnlag)
}

fun kriterieHarIkkeKontaktsadresseIUtlandet(
    kontaktadresser: List<VilkaarOpplysning<Kontaktadresse>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>
): Kriterie {
    val opplysningsGrunnlag = listOf(kontaktadresser, doedsdato).flatten().filter { it.kilde.type == "pdl" }
    val resultat = try {
        val kontaktadresser = hentKontaktadresse(kontaktadresser)
        val doedsdatoPdl = hentDoedsdato(doedsdato).doedsdato
        harKunNorskeAdresserEtterDoedsdato(kontaktadresser.kontaktadresse, doedsdatoPdl)
    } catch (ex: OpplysningKanIkkeHentesUt) {
        VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    }

    return Kriterie(Kriterietyper.SOEKER_IKKE_KONTAKTADRESSE_I_UTLANDET, resultat, opplysningsGrunnlag)
}


fun harKunNorskeAdresserEtterDoedsdato(adresser: List<Adresse>?, doedsdato: LocalDate): VilkaarVurderingsResultat {
    val adresserEtterDoedsdato = adresser?.filter { it.gyldigTilOgMed?.isAfter(doedsdato) == true || it.aktiv }
    val harUtenlandskeAdresserIPdl =
        adresserEtterDoedsdato?.any { it.type == AdresseType.UTENLANDSKADRESSE ||  it.type == AdresseType.UTENLANDSKADRESSEFRITTFORMAT }

    return if (harUtenlandskeAdresserIPdl == true) {
        VilkaarVurderingsResultat.IKKE_OPPFYLT
    } else {
        VilkaarVurderingsResultat.OPPFYLT
    }
}





