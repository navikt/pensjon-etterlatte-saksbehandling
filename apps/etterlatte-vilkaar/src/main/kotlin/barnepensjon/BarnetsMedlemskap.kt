package no.nav.etterlatte.vilkaar.barnepensjon

import no.nav.etterlatte.barnepensjon.OpplysningKanIkkeHentesUt
import no.nav.etterlatte.barnepensjon.hentAdresser
import no.nav.etterlatte.barnepensjon.hentDoedsdato
import no.nav.etterlatte.barnepensjon.opplysningsGrunnlagNull
import no.nav.etterlatte.barnepensjon.setVikaarVurderingFraKriterier
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoekerBarnSoeknad
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import java.time.LocalDate
import java.time.LocalDateTime

fun vilkaarBarnetsMedlemskap(
    vilkaartype: Vilkaartyper,
    soekerPdl: VilkaarOpplysning<Person>?,
    soekerSoeknad: VilkaarOpplysning<SoekerBarnSoeknad>?,
    gjenlevendePdl: VilkaarOpplysning<Person>?,
    avdoedPdl: VilkaarOpplysning<Person>?,
): VurdertVilkaar {

    val barnHarIkkeAdresseIUtlandet =
        kriterieSoekerHarIkkeAdresseIUtlandet(
            soekerPdl,
            soekerSoeknad,
            avdoedPdl,
            Kriterietyper.SOEKER_IKKE_ADRESSE_I_UTLANDET
        )

    val foreldreHarIkkeAdresseIUtlandet = kriterieForeldreHarIkkeAdresseIUtlandet(
        gjenlevendePdl,
        avdoedPdl,
        Kriterietyper.GJENLEVENDE_FORELDER_IKKE_ADRESSE_I_UTLANDET
    )

    return VurdertVilkaar(
        vilkaartype,
        setVikaarVurderingFraKriterier(listOf(barnHarIkkeAdresseIUtlandet, foreldreHarIkkeAdresseIUtlandet)),
        listOf(
            barnHarIkkeAdresseIUtlandet,
            foreldreHarIkkeAdresseIUtlandet
        ),
        LocalDateTime.now()
    )
}

fun kriterieForeldreHarIkkeAdresseIUtlandet(
    gjenlevendePdl: VilkaarOpplysning<Person>?,
    avdoedPdl: VilkaarOpplysning<Person>?,
    kriterietype: Kriterietyper
): Kriterie {
    val opplysningsGrunnlag = listOfNotNull(
        gjenlevendePdl?.let {
            Kriteriegrunnlag(
                KriterieOpplysningsType.ADRESSER,
                gjenlevendePdl.kilde,
                hentAdresser(gjenlevendePdl)
            )
        },
        avdoedPdl?.let {
            Kriteriegrunnlag(
                KriterieOpplysningsType.DOEDSDATO,
                avdoedPdl.kilde,
                Doedsdato(avdoedPdl.opplysning.doedsdato, avdoedPdl.opplysning.foedselsnummer)
            )
        }
    )

    if (gjenlevendePdl == null || avdoedPdl == null) return opplysningsGrunnlagNull(kriterietype, opplysningsGrunnlag)

    val resultat = try {
        kunNorskePdlAdresser(gjenlevendePdl, avdoedPdl, kriterietype)
    } catch (ex: OpplysningKanIkkeHentesUt) {
        VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    }

    return Kriterie(kriterietype, resultat, opplysningsGrunnlag)
}

fun kriterieSoekerHarIkkeAdresseIUtlandet(
    soekerPdl: VilkaarOpplysning<Person>?,
    soekerSoknad: VilkaarOpplysning<SoekerBarnSoeknad>?,
    avdoedPdl: VilkaarOpplysning<Person>?,
    kriterietype: Kriterietyper
): Kriterie {
    val opplysningsGrunnlag = listOfNotNull(
        soekerPdl?.let { Kriteriegrunnlag(KriterieOpplysningsType.ADRESSER, soekerPdl.kilde, hentAdresser(soekerPdl)) },
        soekerSoknad?.let { Kriteriegrunnlag(KriterieOpplysningsType.SOEKER_UTENLANDSOPPHOLD, soekerSoknad.kilde, soekerSoknad.opplysning.utenlandsadresse) },
        avdoedPdl?.let {
            Kriteriegrunnlag(
                KriterieOpplysningsType.DOEDSDATO,
                avdoedPdl.kilde,
                Doedsdato(avdoedPdl.opplysning.doedsdato, avdoedPdl.opplysning.foedselsnummer)
            )
        }
    )

    if (soekerPdl == null || avdoedPdl == null || soekerSoknad == null) return opplysningsGrunnlagNull(
        kriterietype,
        opplysningsGrunnlag
    )

    val resultat = try {
        val pdlResultat = kunNorskePdlAdresser(soekerPdl, avdoedPdl, kriterietype)
        val soeknadResultat =
            if (soekerSoknad.opplysning.utenlandsadresse.adresseIUtlandet == JaNeiVetIkke.JA) {
                VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
            } else VurderingsResultat.OPPFYLT
        val resultater = listOf(pdlResultat, soeknadResultat)

        if (resultater.all { it == VurderingsResultat.OPPFYLT }) {
            VurderingsResultat.OPPFYLT
        } else if (resultater.any { it == VurderingsResultat.IKKE_OPPFYLT }) {
            VurderingsResultat.IKKE_OPPFYLT
        } else {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        }

    } catch (ex: OpplysningKanIkkeHentesUt) {
        VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    }

    return Kriterie(kriterietype, resultat, opplysningsGrunnlag)
}


fun kunNorskePdlAdresser(
    person: VilkaarOpplysning<Person>,
    avdoedPdl: VilkaarOpplysning<Person>,
    kriterietype: Kriterietyper
): VurderingsResultat {
    val adresserPdl = hentAdresser(person)
    val doedsdatoPdl = hentDoedsdato(avdoedPdl)

    val bostedResultat = harKunNorskeAdresserEtterDoedsdato(adresserPdl.bostedadresse, doedsdatoPdl)
    val oppholdResultat = harKunNorskeAdresserEtterDoedsdato(adresserPdl.oppholdadresse, doedsdatoPdl)
    val kontaktResultat = harKunNorskeAdresserEtterDoedsdato(adresserPdl.kontaktadresse, doedsdatoPdl)
    return if (listOf(
            bostedResultat,
            oppholdResultat,
            kontaktResultat
        ).contains(VurderingsResultat.IKKE_OPPFYLT)
    ) {
        if (kriterietype == Kriterietyper.SOEKER_IKKE_ADRESSE_I_UTLANDET && bostedResultat == VurderingsResultat.IKKE_OPPFYLT) {
            VurderingsResultat.IKKE_OPPFYLT
        } else {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        }

    } else {
        VurderingsResultat.OPPFYLT
    }
}


fun harKunNorskeAdresserEtterDoedsdato(adresser: List<Adresse>?, doedsdato: LocalDate): VurderingsResultat {
    val adresserEtterDoedsdato =
        adresser?.filter { it.gyldigTilOgMed?.toLocalDate()?.isAfter(doedsdato) == true || it.aktiv }
    val harUtenlandskeAdresserIPdl =
        adresserEtterDoedsdato?.any { it.type == AdresseType.UTENLANDSKADRESSE || it.type == AdresseType.UTENLANDSKADRESSEFRITTFORMAT }

    return if (harUtenlandskeAdresserIPdl == true) {
        VurderingsResultat.IKKE_OPPFYLT
    } else {
        VurderingsResultat.OPPFYLT
    }
}





