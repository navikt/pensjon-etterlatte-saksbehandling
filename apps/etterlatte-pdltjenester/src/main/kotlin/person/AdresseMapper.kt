package no.nav.etterlatte.person

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.person.Adresse2
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlBostedsadresse
import no.nav.etterlatte.pdl.PdlVegadresse

class AdresseMapper {

    private fun mapBostedsadresse(
        ppsKlient: ParallelleSannheterKlient,
        bostedsadresse: List<PdlBostedsadresse>
    ): List<Adresse2> = runBlocking {

        val aktivBostedsadresse = bostedsadresse
            .filterNot { it.metadata.historisk }
            .let { ppsKlient.avklarBostedsadresse(it) }

        bostedsadresse.map {
            when {
                it.vegadresse != null -> {
                    Adresse2(
                        aktiv = it == aktivBostedsadresse,
                        type = AdresseType.VEGADRESSE,
                        adresseLinje1 = it.coAdressenavn ?: vegAdresseAsAdresseLinje(it.vegadresse),
                        adresseLinje2 = it.coAdressenavn?.let { _ -> vegAdresseAsAdresseLinje(it.vegadresse) },
                        postnr = it.vegadresse.postnummer,
                        kilde = it.metadata.master, // TODO er dette riktig?
                        gyldigFraOgMed = it.gyldigFraOgMed,
                        gyldigTilOgMed = it.gyldigTilOgMed,
                    )
                }
                else -> {
                    Adresse2(
                        aktiv = it == aktivBostedsadresse,
                        type = AdresseType.IKKE_STOETTET,
                        adresseLinje1 = null,
                        adresseLinje2 = null,
                        postnr = null,
                        kilde = "",
                        gyldigFraOgMed = null,
                        gyldigTilOgMed = null,
                    )
                }
            }
        }
    }

    private fun vegAdresseAsAdresseLinje(vegadresse: PdlVegadresse) = listOfNotNull(
        vegadresse.adressenavn,
        vegadresse.husnummer,
        vegadresse.husbokstav
    ).joinToString(" ")

}

