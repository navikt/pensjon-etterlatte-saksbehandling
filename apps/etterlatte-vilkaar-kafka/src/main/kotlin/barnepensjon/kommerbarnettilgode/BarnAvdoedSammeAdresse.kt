package vilkaar.barnepensjon

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.barnepensjon.OpplysningKanIkkeHentesUt
import no.nav.etterlatte.barnepensjon.setVilkaarVurderingFraKriterier
import no.nav.etterlatte.barnepensjon.vurderOpplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Bostedadresser
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.hentBostedsadresse
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import java.time.LocalDateTime

fun barnOgAvdoedSammeBostedsadresse(
    soekerPdl: Grunnlagsdata<JsonNode>?,
    avdoedPdl: Grunnlagsdata<JsonNode>?
): VurdertVilkaar {
    val sammeBostedsAdresse = kriterieSammeBostedsadresseSomAvdoed(soekerPdl, avdoedPdl)

    return VurdertVilkaar(
        Vilkaartyper.BARN_BOR_PAA_AVDOEDES_ADRESSE,
        setVilkaarVurderingFraKriterier(listOf(sammeBostedsAdresse)),
        null,
        listOf(sammeBostedsAdresse),
        LocalDateTime.now()
    )
}

fun kriterieSammeBostedsadresseSomAvdoed(
    soekerPdl: Grunnlagsdata<JsonNode>?,
    avdoedPdl: Grunnlagsdata<JsonNode>?
): Kriterie {
    val soekerAdresse = soekerPdl?.hentBostedsadresse()
    val avdoedAdresse = avdoedPdl?.hentBostedsadresse()
    val opplysningsGrunnlag = listOfNotNull(
        soekerAdresse?.let {
            Kriteriegrunnlag(
                it.id,
                KriterieOpplysningsType.BOSTEDADRESSE_SOEKER,
                it.kilde,
                Bostedadresser(it.verdi)
            )
        },
        avdoedAdresse?.let {
            Kriteriegrunnlag(
                it.id,
                KriterieOpplysningsType.BOSTEDADRESSE_AVDOED,
                it.kilde,
                Bostedadresser(it.verdi)
            )
        }
    )

    val resultat = try {
        if (soekerAdresse == null || avdoedAdresse == null) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        } else {
            val adresseBarn = soekerAdresse.verdi.find { it.aktiv }

            fun hentAktivEllerSisteAdresse(): Adresse? {
                if (avdoedAdresse.verdi.find { it.aktiv } != null) {
                    return avdoedAdresse.verdi.find { it.aktiv }
                } else {
                    return avdoedAdresse.verdi.sortedByDescending { it.gyldigFraOgMed?.toLocalDate() }.first()
                }
            }

            val sisteAdresseAvdoed = hentAktivEllerSisteAdresse()

            val adresse1 = adresseBarn?.adresseLinje1 == sisteAdresseAvdoed?.adresseLinje1
            val postnr = adresseBarn?.postnr == sisteAdresseAvdoed?.postnr
            val poststed = adresseBarn?.poststed == sisteAdresseAvdoed?.poststed
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