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
    val opplysningsGrunnlag = listOfNotNull( // TODO ai: fiks
        soekerAdresse?.let {
            Kriteriegrunnlag(
                it.perioder.last().id,
                KriterieOpplysningsType.BOSTEDADRESSE_SOEKER,
                it.perioder.last().kilde,
                Bostedadresser(soekerAdresse.perioder.map { it.verdi }) // TODO ai: periodisering
            )
        },
        avdoedAdresse?.let {
            Kriteriegrunnlag(
                it.perioder.last().id,
                KriterieOpplysningsType.BOSTEDADRESSE_AVDOED,
                it.perioder.last().kilde,
                Bostedadresser(avdoedAdresse.perioder.map { it.verdi }) // TODO ai: periodisering
            )
        }
    )

    val resultat = try {
        if (soekerAdresse == null || avdoedAdresse == null) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        } else {
            val adresseBarn = soekerAdresse.perioder.find { it.verdi.aktiv }

            fun hentAktivEllerSisteAdresse(): Adresse? {
                val aktivAdresse = avdoedAdresse.perioder.find { it.verdi.aktiv }?.verdi
                return aktivAdresse ?: avdoedAdresse.perioder
                    .sortedByDescending { it.verdi.gyldigFraOgMed?.toLocalDate() }.first().verdi
            }

            val sisteAdresseAvdoed = hentAktivEllerSisteAdresse()

            val adresse1 = adresseBarn?.verdi?.adresseLinje1 == sisteAdresseAvdoed?.adresseLinje1
            val postnr = adresseBarn?.verdi?.postnr == sisteAdresseAvdoed?.postnr
            val poststed = adresseBarn?.verdi?.poststed == sisteAdresseAvdoed?.poststed
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