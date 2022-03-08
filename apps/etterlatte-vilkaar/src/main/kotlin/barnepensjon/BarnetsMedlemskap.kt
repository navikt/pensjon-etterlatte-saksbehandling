package no.nav.etterlatte.vilkaar.barnepensjon

import no.nav.etterlatte.barnepensjon.OpplysningKanIkkeHentesUt
import no.nav.etterlatte.barnepensjon.hentBostedsadresse
import no.nav.etterlatte.barnepensjon.hentDoedsdato
import no.nav.etterlatte.barnepensjon.hentOppholdadresse
import no.nav.etterlatte.barnepensjon.hentUtenlandsadresseSoeknad
import no.nav.etterlatte.barnepensjon.setVikaarVurderingsResultat
import no.nav.etterlatte.barnepensjon.vurderOpplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Adresse
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Bostedadresse
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Oppholdadresse
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Utenlandsadresse
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
        setVikaarVurderingsResultat(listOf(harIkkeBostedadresseIUtlandetPdl, harIkkeBostedadresseIUtlandetSoeknad)),
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
        vurderOpplysning { hentUtenlandsadresseSoeknad(utenlandsadresse).adresseIUtlandet?.lowercase() == "nei" }

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
    val adresserEtterDoedsdato = adresser?.filter { it.gyldigTilOgMed?.isAfter(doedsdato) == true || it.aktiv }

    if (adresserEtterDoedsdato != null && adresserEtterDoedsdato.any { it.land == null }) return VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING

    val harUtenlandskeAdresserIPdl =
        adresserEtterDoedsdato?.map { it.land?.lowercase() == "norge" || it.land?.lowercase() == "nor" }
            ?.contains(false)

    return if (harUtenlandskeAdresserIPdl == null) {
        VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    } else if (harUtenlandskeAdresserIPdl == false) {
        VilkaarVurderingsResultat.OPPFYLT
    } else {
        VilkaarVurderingsResultat.IKKE_OPPFYLT
    }
}





