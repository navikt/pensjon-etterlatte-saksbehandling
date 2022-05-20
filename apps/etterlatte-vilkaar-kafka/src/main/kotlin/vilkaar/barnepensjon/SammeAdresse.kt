package vilkaar.barnepensjon

import no.nav.etterlatte.barnepensjon.OpplysningKanIkkeHentesUt
import no.nav.etterlatte.barnepensjon.hentBostedsAdresser
import no.nav.etterlatte.barnepensjon.setVikaarVurderingFraKriterier
import no.nav.etterlatte.barnepensjon.vurderOpplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Bostedadresser
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

fun ekstraVilkaarBarnOgForelderSammeBostedsadresse(
    vilkaartype: Vilkaartyper,
    soekerPdl: VilkaarOpplysning<Person>?,
    gjenlevendePdl: VilkaarOpplysning<Person>?
): VurdertVilkaar {
    val sammeBostedsAdresse = kriterieSammeBostedsadresse(soekerPdl, gjenlevendePdl)

    return VurdertVilkaar(
        vilkaartype,
        setVikaarVurderingFraKriterier(listOf(sammeBostedsAdresse)),
        listOf(sammeBostedsAdresse),
        LocalDateTime.now()
    )
}

fun kriterieSammeBostedsadresse(
    soekerPdl: VilkaarOpplysning<Person>?,
    gjenlevendePdl: VilkaarOpplysning<Person>?
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
        gjenlevendePdl?.let {
            Kriteriegrunnlag(
                gjenlevendePdl.id,
                KriterieOpplysningsType.BOSTEDADRESSE_GJENLEVENDE,
                gjenlevendePdl.kilde,
                Bostedadresser(gjenlevendePdl.opplysning.bostedsadresse)
            )
        }
    )

    val resultat = try {
        if (gjenlevendePdl == null || soekerPdl == null) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        } else {
            val adresseBarn = hentBostedsAdresser(soekerPdl).find { it.aktiv }
            val adresseGjenlevende = hentBostedsAdresser(gjenlevendePdl).find { it.aktiv }

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


