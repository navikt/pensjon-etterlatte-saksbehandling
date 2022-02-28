package no.nav.etterlatte.pdl.mapper

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.person.Adresse2
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlBostedsadresse
import no.nav.etterlatte.pdl.PdlVegadresse

object AdresseMapper {

    fun mapBostedsadresse(
        ppsKlient: ParallelleSannheterKlient,
        bostedsadresse: List<PdlBostedsadresse>
    ): List<Adresse2> = runBlocking {

        val aktivBostedsadresse = bostedsadresse
            .filterNot { it.metadata.historisk }
            .let { ppsKlient.avklarBostedsadresse(it) }

        bostedsadresse.map {

            fun toAdresse(
                type: AdresseType,
                adresseLinje1: String? = null,
                adresseLinje2: String? = null,
                postnr: String? = null,
                poststed: String? = null
            ) = Adresse2(
                    aktiv = it == aktivBostedsadresse,
                    type = type,
                    adresseLinje1 = adresseLinje1,
                    adresseLinje2 = adresseLinje2,
                    postnr = postnr,
                    poststed = poststed,
                    kilde = it.metadata.master,
                    gyldigFraOgMed = it.gyldigFraOgMed,
                    gyldigTilOgMed = it.gyldigTilOgMed,
                )

            when {
                it.vegadresse != null -> {
                    toAdresse(
                        type = AdresseType.VEGADRESSE,
                        adresseLinje1 = it.coAdressenavn ?: vegAdresseAsAdresseLinje(it.vegadresse),
                        adresseLinje2 = it.coAdressenavn?.let { _ -> vegAdresseAsAdresseLinje(it.vegadresse) },
                        postnr = it.vegadresse.postnummer,
                    )
                }

                it.utenlandskAdresse != null -> {
                    toAdresse(
                        type = AdresseType.UTENLANDSKADRESSE,
                        adresseLinje1 = listOfNotNull(
                            it.coAdressenavn,
                            it.utenlandskAdresse.adressenavnNummer
                        ).joinToString(" "),
                        adresseLinje2 = listOfNotNull(
                            it.utenlandskAdresse.bygningEtasjeLeilighet,
                            it.utenlandskAdresse.postboksNummerNavn
                        ).joinToString(" "),
                        postnr = it.utenlandskAdresse.postkode,
                        poststed = it.utenlandskAdresse.bySted,
                    )
                }

                it.ukjentBosted != null -> {
                    toAdresse(
                        type = AdresseType.UKJENT_BOSTED,
                        adresseLinje1 = it.coAdressenavn ?: it.ukjentBosted.bostedskommune,
                        adresseLinje2 = it.coAdressenavn?.let { _ -> it.ukjentBosted.bostedskommune },
                    )
                }

                it.matrikkeladresse != null -> {
                    toAdresse(
                        type = AdresseType.MATRIKKELADRESSE,
                        postnr = it.matrikkeladresse.postnummer,
                    )
                }

                else -> {
                    toAdresse(
                        type = AdresseType.IKKE_STOETTET,
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

