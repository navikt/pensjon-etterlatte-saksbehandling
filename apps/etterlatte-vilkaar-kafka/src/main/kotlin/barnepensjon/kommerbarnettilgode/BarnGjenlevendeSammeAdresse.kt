package vilkaar.barnepensjon

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.barnepensjon.OpplysningKanIkkeHentesUt
import no.nav.etterlatte.barnepensjon.setVilkaarVurderingFraKriterier
import no.nav.etterlatte.barnepensjon.vurderOpplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Bostedadresser
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.hentBostedsadresse
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import java.time.LocalDateTime

fun barnOgForelderSammeBostedsadresse(
    soekerPdl: Grunnlagsdata<JsonNode>?,
    gjenlevendePdl: Grunnlagsdata<JsonNode>?
): VurdertVilkaar {
    val sammeBostedsAdresse = kriterieSammeBostedsadresse(soekerPdl, gjenlevendePdl)

    return VurdertVilkaar(
        Vilkaartyper.GJENLEVENDE_OG_BARN_SAMME_BOSTEDADRESSE,
        setVilkaarVurderingFraKriterier(listOf(sammeBostedsAdresse)),
        null,
        listOf(sammeBostedsAdresse),
        LocalDateTime.now()
    )
}

fun kriterieSammeBostedsadresse(
    soekerPdl: Grunnlagsdata<JsonNode>?,
    gjenlevendePdl: Grunnlagsdata<JsonNode>?
): Kriterie {
    val søkersAdresse = soekerPdl?.hentBostedsadresse()
    val gjenlevendesAdresse = gjenlevendePdl?.hentBostedsadresse()

    val opplysningsGrunnlag = listOfNotNull(
        søkersAdresse?.let {
            Kriteriegrunnlag(
                søkersAdresse.id,
                KriterieOpplysningsType.BOSTEDADRESSE_SOEKER,
                søkersAdresse.kilde,
                Bostedadresser(søkersAdresse.verdi)
            )
        },
        gjenlevendesAdresse?.let {
            Kriteriegrunnlag(
                gjenlevendesAdresse.id,
                KriterieOpplysningsType.BOSTEDADRESSE_GJENLEVENDE,
                gjenlevendesAdresse.kilde,
                Bostedadresser(gjenlevendesAdresse.verdi)
            )
        }
    )

    val resultat = try {
        if (gjenlevendesAdresse == null || søkersAdresse == null) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        } else {
            val adresseBarn = søkersAdresse.verdi.find { it.aktiv }
            val adresseGjenlevende = gjenlevendesAdresse.verdi.find { it.aktiv }

            val adresse1 = adresseBarn?.adresseLinje1 == adresseGjenlevende?.adresseLinje1
            val postnr = adresseBarn?.postnr == adresseGjenlevende?.postnr
            val poststed = adresseBarn?.poststed == adresseGjenlevende?.poststed
            vurderOpplysning { adresse1 && postnr && poststed }
        }
    } catch (ex: OpplysningKanIkkeHentesUt) {
        VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    }

    return Kriterie(
        Kriterietyper.SAMME_BOSTEDSADRESSE,
        resultat,
        opplysningsGrunnlag
    )
}