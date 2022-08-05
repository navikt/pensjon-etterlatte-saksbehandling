package vilkaar.barnepensjon

import no.nav.etterlatte.barnepensjon.OpplysningKanIkkeHentesUt
import no.nav.etterlatte.barnepensjon.hentBostedsAdresser
import no.nav.etterlatte.barnepensjon.setVilkaarVurderingFraKriterier
import no.nav.etterlatte.barnepensjon.vurderOpplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Bostedadresser
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import java.time.LocalDateTime

fun barnOgAvdoedSammeBostedsadresse(
        soekerPdl: VilkaarOpplysning<Person>?,
        avdoedPdl: VilkaarOpplysning<Person>?
) : VurdertVilkaar {
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
    soekerPdl: VilkaarOpplysning<Person>?,
    avdoedPdl: VilkaarOpplysning<Person>?
): Kriterie {
    val opplysningsGrunnlag = listOfNotNull(
        soekerPdl?.let {
            Kriteriegrunnlag(
                soekerPdl.id,
                KriterieOpplysningsType.BOSTEDADRESSE_SOEKER,
                soekerPdl.kilde,
                Bostedadresser(soekerPdl.opplysning.bostedsadresse)
            )
        },
        avdoedPdl?.let {
            Kriteriegrunnlag(
                avdoedPdl.id,
                KriterieOpplysningsType.BOSTEDADRESSE_AVDOED,
                avdoedPdl.kilde,
                Bostedadresser(avdoedPdl.opplysning.bostedsadresse)
            )
        }
    )

    val resultat = try {
        if (avdoedPdl == null || soekerPdl == null) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        } else {
            val adresseBarn = hentBostedsAdresser(soekerPdl).find { it.aktiv }
            val adresserAvdoed = hentBostedsAdresser(avdoedPdl)

            fun hentAktivEllerSisteAdresse() : Adresse? {
                if (adresserAvdoed.find { it.aktiv } != null)
                    return adresserAvdoed.find { it.aktiv }
                else {
                    return adresserAvdoed.sortedByDescending { it.gyldigFraOgMed?.toLocalDate() }.first()
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