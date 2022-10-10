package no.nav.etterlatte.vilkaar.barnepensjon

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.barnepensjon.OpplysningKanIkkeHentesUt
import no.nav.etterlatte.barnepensjon.harKunNorskePdlAdresserEtterDato
import no.nav.etterlatte.barnepensjon.hentAdresser
import no.nav.etterlatte.barnepensjon.opplysningsGrunnlagNull
import no.nav.etterlatte.barnepensjon.setVilkaarVurderingFraKriterier
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.hentBostedsadresse
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentUtenlandsadresse
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import java.time.LocalDateTime

fun vilkaarBarnetsMedlemskap(
    soeker: Grunnlagsdata<JsonNode>?,
    gjenlevende: Grunnlagsdata<JsonNode>?,
    avdoed: Grunnlagsdata<JsonNode>?
): VurdertVilkaar {
    val barnHarIkkeAdresseIUtlandet =
        kriterieSoekerHarIkkeAdresseIUtlandet(
            soeker,
            avdoed,
            Kriterietyper.SOEKER_IKKE_ADRESSE_I_UTLANDET
        )

    val foreldreHarIkkeAdresseIUtlandet = kriterieForeldreHarIkkeAdresseIUtlandet(
        gjenlevende,
        avdoed,
        Kriterietyper.GJENLEVENDE_FORELDER_IKKE_ADRESSE_I_UTLANDET
    )

    return VurdertVilkaar(
        Vilkaartyper.BARNETS_MEDLEMSKAP,
        setVilkaarVurderingFraKriterier(listOf(barnHarIkkeAdresseIUtlandet, foreldreHarIkkeAdresseIUtlandet)),
        null,
        listOf(
            barnHarIkkeAdresseIUtlandet,
            foreldreHarIkkeAdresseIUtlandet
        ),
        LocalDateTime.now()
    )
}

fun kriterieForeldreHarIkkeAdresseIUtlandet(
    gjenlevende: Grunnlagsdata<JsonNode>?,
    avdoed: Grunnlagsdata<JsonNode>?,
    kriterietype: Kriterietyper
): Kriterie {
    val doedsdatoAvdoed = avdoed?.hentDoedsdato()
    val adresseGjenlevende = gjenlevende?.hentBostedsadresse()

    val opplysningsGrunnlag = listOfNotNull(
        adresseGjenlevende?.hentSenest()?.let {
            Kriteriegrunnlag(
                it.id,
                KriterieOpplysningsType.ADRESSER,
                it.kilde,
                hentAdresser(gjenlevende)
            )
        },
        doedsdatoAvdoed?.let {
            Kriteriegrunnlag(
                it.id,
                KriterieOpplysningsType.DOEDSDATO,
                it.kilde,
                Doedsdato(it.verdi, avdoed.hentFoedselsnummer()?.verdi!!)
            )
        }
    )

    if (gjenlevende == null || avdoed == null) return opplysningsGrunnlagNull(kriterietype, opplysningsGrunnlag)

    val resultat = try {
        val gjenlevendeAdresser = hentAdresser(gjenlevende)
        val adresserResult = harKunNorskePdlAdresserEtterDato(gjenlevendeAdresser, doedsdatoAvdoed?.verdi!!)
        if (adresserResult == VurderingsResultat.IKKE_OPPFYLT) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        } else {
            adresserResult
        }
    } catch (ex: OpplysningKanIkkeHentesUt) {
        VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    }

    return Kriterie(kriterietype, resultat, opplysningsGrunnlag)
}

fun kriterieSoekerHarIkkeAdresseIUtlandet(
    soeker: Grunnlagsdata<JsonNode>?,
    avdoed: Grunnlagsdata<JsonNode>?,
    kriterietype: Kriterietyper
): Kriterie {
    val doedsdatoAvdoed = avdoed?.hentDoedsdato()

    val opplysningsGrunnlag = listOfNotNull(
        soeker?.hentBostedsadresse()?.hentSenest()?.let {
            Kriteriegrunnlag(
                it.id,
                KriterieOpplysningsType.ADRESSER,
                it.kilde,
                hentAdresser(soeker)
            )
        },
        soeker?.hentUtenlandsadresse()?.let {
            Kriteriegrunnlag(
                it.id,
                KriterieOpplysningsType.SOEKER_UTENLANDSOPPHOLD,
                it.kilde,
                it.verdi
            )
        },
        doedsdatoAvdoed?.let {
            Kriteriegrunnlag(
                doedsdatoAvdoed.id,
                KriterieOpplysningsType.DOEDSDATO,
                doedsdatoAvdoed.kilde,
                Doedsdato(doedsdatoAvdoed.verdi, avdoed.hentFoedselsnummer()?.verdi!!)
            )
        }
    )

    if (soeker == null || doedsdatoAvdoed?.verdi == null) {
        return opplysningsGrunnlagNull(
            kriterietype,
            opplysningsGrunnlag
        )
    }

    val resultat = try {
        val soekerAdresserPdl = hentAdresser(soeker)
        val pdlResultat = harKunNorskePdlAdresserEtterDato(soekerAdresserPdl, doedsdatoAvdoed.verdi!!)
        val soeknadResultat =
            if (soeker.hentUtenlandsadresse()?.verdi?.harHattUtenlandsopphold == JaNeiVetIkke.JA) {
                VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
            } else {
                VurderingsResultat.OPPFYLT
            }
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