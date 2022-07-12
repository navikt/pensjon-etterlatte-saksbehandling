package no.nav.etterlatte.vilkaar.barnepensjon

import no.nav.etterlatte.barnepensjon.OpplysningKanIkkeHentesUt
import no.nav.etterlatte.barnepensjon.hentAdresser
import no.nav.etterlatte.barnepensjon.hentDoedsdato
import no.nav.etterlatte.barnepensjon.harKunNorskePdlAdresserEtterDato
import no.nav.etterlatte.barnepensjon.opplysningsGrunnlagNull
import no.nav.etterlatte.barnepensjon.setVikaarVurderingFraKriterier
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoekerBarnSoeknad
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
        null,
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
                gjenlevendePdl.id,
                KriterieOpplysningsType.ADRESSER,
                gjenlevendePdl.kilde,
                hentAdresser(gjenlevendePdl)
            )
        },
        avdoedPdl?.let {
            Kriteriegrunnlag(
                avdoedPdl.id,
                KriterieOpplysningsType.DOEDSDATO,
                avdoedPdl.kilde,
                Doedsdato(avdoedPdl.opplysning.doedsdato, avdoedPdl.opplysning.foedselsnummer)
            )
        }
    )

    if (gjenlevendePdl == null || avdoedPdl == null) return opplysningsGrunnlagNull(kriterietype, opplysningsGrunnlag)

    val resultat = try {
        val gjenlevendeAdresser = hentAdresser(gjenlevendePdl)
        val doedsdato = hentDoedsdato(avdoedPdl)
        val adresserResult = harKunNorskePdlAdresserEtterDato(gjenlevendeAdresser, doedsdato)
        if (adresserResult == VurderingsResultat.IKKE_OPPFYLT) VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING else adresserResult

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
        soekerPdl?.let {
            Kriteriegrunnlag(
                soekerPdl.id,
                KriterieOpplysningsType.ADRESSER,
                soekerPdl.kilde,
                hentAdresser(soekerPdl)
            )
        },
        soekerSoknad?.let {
            Kriteriegrunnlag(
                soekerSoknad.id,
                KriterieOpplysningsType.SOEKER_UTENLANDSOPPHOLD,
                soekerSoknad.kilde,
                soekerSoknad.opplysning.utenlandsadresse
            )
        },
        avdoedPdl?.let {
            Kriteriegrunnlag(
                avdoedPdl.id,
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
        val doedsdato = hentDoedsdato(avdoedPdl)
        val soekerAdresserPdl = hentAdresser(soekerPdl)
        val pdlResultat = harKunNorskePdlAdresserEtterDato(soekerAdresserPdl, doedsdato)
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


