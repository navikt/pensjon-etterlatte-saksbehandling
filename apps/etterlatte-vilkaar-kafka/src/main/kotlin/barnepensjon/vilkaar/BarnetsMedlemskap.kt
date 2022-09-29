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
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoekerBarnSoeknad
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import java.time.LocalDateTime

fun vilkaarBarnetsMedlemskap(
    soekerPdl: Grunnlagsdata<JsonNode>?,
    soekerSoeknad: VilkaarOpplysning<SoekerBarnSoeknad>?,
    gjenlevendePdl: Grunnlagsdata<JsonNode>?,
    avdoedPdl: Grunnlagsdata<JsonNode>?
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
    gjenlevendePdl: Grunnlagsdata<JsonNode>?,
    avdoedPdl: Grunnlagsdata<JsonNode>?,
    kriterietype: Kriterietyper
): Kriterie {
    val dødsdatoAvdød = avdoedPdl?.hentDoedsdato()
    val adresseGjenlevende = gjenlevendePdl?.hentBostedsadresse()

    val opplysningsGrunnlag = listOfNotNull(
        adresseGjenlevende?.let {
            Kriteriegrunnlag(
                it.id,
                KriterieOpplysningsType.ADRESSER,
                it.kilde,
                hentAdresser(gjenlevendePdl)
            )
        },
        dødsdatoAvdød?.let {
            Kriteriegrunnlag(
                it.id,
                KriterieOpplysningsType.DOEDSDATO,
                it.kilde,
                Doedsdato(it.verdi, avdoedPdl.hentFoedselsnummer()?.verdi!!)
            )
        }
    )

    if (gjenlevendePdl == null || avdoedPdl == null) return opplysningsGrunnlagNull(kriterietype, opplysningsGrunnlag)

    val resultat = try {
        val gjenlevendeAdresser = hentAdresser(gjenlevendePdl)
        val adresserResult = harKunNorskePdlAdresserEtterDato(gjenlevendeAdresser, dødsdatoAvdød?.verdi!!)
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
    soekerPdl: Grunnlagsdata<JsonNode>?,
    soekerSoknad: VilkaarOpplysning<SoekerBarnSoeknad>?,
    avdoedPdl: Grunnlagsdata<JsonNode>?,
    kriterietype: Kriterietyper
): Kriterie {
    val dødsdatoAvdød = avdoedPdl?.hentDoedsdato()

    val opplysningsGrunnlag = listOfNotNull(
        soekerPdl?.hentBostedsadresse()?.let {
            Kriteriegrunnlag(
                it.id,
                KriterieOpplysningsType.ADRESSER,
                it.kilde,
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
        dødsdatoAvdød?.let {
            Kriteriegrunnlag(
                dødsdatoAvdød.id,
                KriterieOpplysningsType.DOEDSDATO,
                dødsdatoAvdød.kilde,
                Doedsdato(dødsdatoAvdød.verdi, avdoedPdl.hentFoedselsnummer()?.verdi!!)
            )
        }
    )

    if (soekerPdl == null || dødsdatoAvdød?.verdi == null || soekerSoknad == null) {
        return opplysningsGrunnlagNull(
            kriterietype,
            opplysningsGrunnlag
        )
    }

    val resultat = try {
        val soekerAdresserPdl = hentAdresser(soekerPdl)
        val pdlResultat = harKunNorskePdlAdresserEtterDato(soekerAdresserPdl, dødsdatoAvdød.verdi!!)
        val soeknadResultat =
            if (soekerSoknad.opplysning.utenlandsadresse.adresseIUtlandet == JaNeiVetIkke.JA) {
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