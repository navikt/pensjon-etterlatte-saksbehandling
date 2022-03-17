package no.nav.etterlatte.vilkaar.barnepensjon

import no.nav.etterlatte.barnepensjon.OpplysningKanIkkeHentesUt
import no.nav.etterlatte.barnepensjon.hentAdresser
import no.nav.etterlatte.barnepensjon.hentDoedsdato
import no.nav.etterlatte.barnepensjon.setVikaarVurderingsResultat
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VilkaarVurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import java.time.LocalDate

fun vilkaarBarnetsMedlemskap(
    vilkaartype: Vilkaartyper,
    soekerPdl: VilkaarOpplysning<Person>,
    gjenlevendePdl: VilkaarOpplysning<Person>,
    avdoedPdl: VilkaarOpplysning<Person>,
): VurdertVilkaar {
    //TODO legg adresse til fra søknad
    val barnHarIkkeAdresseIUtlandet =
        kriterieHarIkkeAdresseIUtlandet(soekerPdl, avdoedPdl, Kriterietyper.SOEKER_IKKE_ADRESSE_I_UTLANDET)

    val foreldreHarIkkeAdresseIUtlandet = kriterieHarIkkeAdresseIUtlandet(
        gjenlevendePdl,
        avdoedPdl,
        Kriterietyper.GJENLEVENDE_FORELDER_IKKE_ADRESSE_I_UTLANDET
    )

    return VurdertVilkaar(
        vilkaartype,
        setVikaarVurderingsResultat(listOf(barnHarIkkeAdresseIUtlandet, foreldreHarIkkeAdresseIUtlandet)),
        listOf(
            barnHarIkkeAdresseIUtlandet,
            foreldreHarIkkeAdresseIUtlandet
        )
    )
}

fun kriterieHarIkkeAdresseIUtlandet(
    person: VilkaarOpplysning<Person>,
    avdoedPdl: VilkaarOpplysning<Person>,
    kriterietype: Kriterietyper
): Kriterie {
    val resultat = try {
        val adresserPdl = hentAdresser(person)
        val doedsdatoPdl = hentDoedsdato(avdoedPdl)

        val bostedResultat = harKunNorskeAdresserEtterDoedsdato(adresserPdl.bostedadresse, doedsdatoPdl)
        val oppholdResultat = harKunNorskeAdresserEtterDoedsdato(adresserPdl.oppholdadresse, doedsdatoPdl)
        val kontaktResultat = harKunNorskeAdresserEtterDoedsdato(adresserPdl.kontaktadresse, doedsdatoPdl)
        if (listOf(bostedResultat, oppholdResultat, kontaktResultat).contains(VilkaarVurderingsResultat.IKKE_OPPFYLT)) {
            if (kriterietype == Kriterietyper.SOEKER_IKKE_ADRESSE_I_UTLANDET) {
                VilkaarVurderingsResultat.IKKE_OPPFYLT
            } else {
                VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
            }

        } else {
            VilkaarVurderingsResultat.OPPFYLT
        }

    } catch (ex: OpplysningKanIkkeHentesUt) {
        VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    }

    val opplysningsGrunnlag = listOf(
        Kriteriegrunnlag(person.kilde, hentAdresser(person)),
        Kriteriegrunnlag(
            avdoedPdl.kilde,
            Doedsdato(avdoedPdl.opplysning.doedsdato, avdoedPdl.opplysning.foedselsnummer)
        )
    )

    return Kriterie(kriterietype, resultat, opplysningsGrunnlag)
}


/*
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
*/


fun harKunNorskeAdresserEtterDoedsdato(adresser: List<Adresse>?, doedsdato: LocalDate): VilkaarVurderingsResultat {
    val adresserEtterDoedsdato =
        adresser?.filter { it.gyldigTilOgMed?.toLocalDate()?.isAfter(doedsdato) == true || it.aktiv }
    val harUtenlandskeAdresserIPdl =
        adresserEtterDoedsdato?.any { it.type == AdresseType.UTENLANDSKADRESSE || it.type == AdresseType.UTENLANDSKADRESSEFRITTFORMAT }

    return if (harUtenlandskeAdresserIPdl == true) {
        VilkaarVurderingsResultat.IKKE_OPPFYLT
    } else {
        VilkaarVurderingsResultat.OPPFYLT
    }
}





