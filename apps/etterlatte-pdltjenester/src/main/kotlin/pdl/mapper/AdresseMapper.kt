package no.nav.etterlatte.pdl.mapper

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlBostedsadresse
import no.nav.etterlatte.pdl.PdlDeltBostedsadresse
import no.nav.etterlatte.pdl.PdlKontaktadresse
import no.nav.etterlatte.pdl.PdlOppholdsadresse
import no.nav.etterlatte.pdl.PdlVegadresse

object AdresseMapper {
    fun mapBostedsadresse(
        ppsKlient: ParallelleSannheterKlient,
        bostedsadresse: List<PdlBostedsadresse>,
    ): List<Adresse> =
        runBlocking {
            val aktivBostedsadresse =
                bostedsadresse
                    .filterNot { it.metadata.historisk }
                    .let { ppsKlient.avklarBostedsadresse(it) }

            bostedsadresse.map {
                fun toAdresse(
                    type: AdresseType,
                    adresseLinje1: String? = null,
                    adresseLinje2: String? = null,
                    adresseLinje3: String? = null,
                    postnr: String? = null,
                    poststed: String? = null,
                    land: String? = null,
                ) = Adresse(
                    aktiv = it == aktivBostedsadresse,
                    type = type,
                    coAdresseNavn = it.coAdressenavn,
                    adresseLinje1 = adresseLinje1,
                    adresseLinje2 = adresseLinje2,
                    adresseLinje3 = adresseLinje3,
                    postnr = postnr,
                    poststed = poststed,
                    land = land,
                    kilde = it.metadata.master,
                    gyldigFraOgMed = it.gyldigFraOgMed,
                    gyldigTilOgMed = it.gyldigTilOgMed,
                )

                when {
                    it.vegadresse != null -> {
                        toAdresse(
                            type = AdresseType.VEGADRESSE,
                            adresseLinje1 = vegAdresseAsAdresseLinje(it.vegadresse),
                            postnr = it.vegadresse.postnummer,
                        )
                    }

                    it.utenlandskAdresse != null -> {
                        toAdresse(
                            type = AdresseType.UTENLANDSKADRESSE,
                            adresseLinje1 = it.utenlandskAdresse.adressenavnNummer,
                            adresseLinje2 =
                                listOfNotNull(
                                    it.utenlandskAdresse.bygningEtasjeLeilighet,
                                    it.utenlandskAdresse.postboksNummerNavn,
                                ).joinToString(" "),
                            postnr = it.utenlandskAdresse.postkode,
                            poststed = it.utenlandskAdresse.bySted,
                            land = it.utenlandskAdresse.landkode,
                        )
                    }

                    it.ukjentBosted != null -> {
                        toAdresse(
                            type = AdresseType.UKJENT_BOSTED,
                            adresseLinje1 = it.ukjentBosted.bostedskommune,
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
                            type = AdresseType.UKJENT,
                        )
                    }
                }
            }
        }

    fun mapDeltBostedsadresse(
        ppsKlient: ParallelleSannheterKlient,
        deltbostedsadresse: List<PdlDeltBostedsadresse>,
    ): List<Adresse> =
        runBlocking {
            val aktivBostedsadresse =
                deltbostedsadresse
                    .filterNot { it.metadata.historisk }
                    .let { ppsKlient.avklarDeltBostedsadresse(it) }

            deltbostedsadresse.map {
                fun toAdresse(
                    type: AdresseType,
                    adresseLinje1: String? = null,
                    adresseLinje2: String? = null,
                    adresseLinje3: String? = null,
                    postnr: String? = null,
                    poststed: String? = null,
                    land: String? = null,
                ) = Adresse(
                    aktiv = it == aktivBostedsadresse,
                    type = type,
                    coAdresseNavn = it.coAdressenavn,
                    adresseLinje1 = adresseLinje1,
                    adresseLinje2 = adresseLinje2,
                    adresseLinje3 = adresseLinje3,
                    postnr = postnr,
                    poststed = poststed,
                    land = land,
                    kilde = it.metadata.master,
                    gyldigFraOgMed = it.startdatoForKontrakt,
                    gyldigTilOgMed = it.sluttdatoForKontrakt,
                )

                when {
                    it.vegadresse != null -> {
                        toAdresse(
                            type = AdresseType.VEGADRESSE,
                            adresseLinje1 = vegAdresseAsAdresseLinje(it.vegadresse),
                            postnr = it.vegadresse.postnummer,
                        )
                    }

                    it.utenlandskAdresse != null -> {
                        toAdresse(
                            type = AdresseType.UTENLANDSKADRESSE,
                            adresseLinje1 = it.utenlandskAdresse.adressenavnNummer,
                            adresseLinje2 =
                                listOfNotNull(
                                    it.utenlandskAdresse.bygningEtasjeLeilighet,
                                    it.utenlandskAdresse.postboksNummerNavn,
                                ).joinToString(" "),
                            postnr = it.utenlandskAdresse.postkode,
                            poststed = it.utenlandskAdresse.bySted,
                            land = it.utenlandskAdresse.landkode,
                        )
                    }

                    it.ukjentBosted != null -> {
                        toAdresse(
                            type = AdresseType.UKJENT_BOSTED,
                            adresseLinje1 = it.ukjentBosted.bostedskommune,
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
                            type = AdresseType.UKJENT,
                        )
                    }
                }
            }
        }

    fun mapOppholdsadresse(
        ppsKlient: ParallelleSannheterKlient,
        oppholdsadresse: List<PdlOppholdsadresse>,
    ): List<Adresse> =
        runBlocking {
            val aktivOppholdsadresse =
                oppholdsadresse
                    .filterNot { it.metadata.historisk }
                    .let { ppsKlient.avklarOppholdsadresse(it) }

            oppholdsadresse.map {
                fun toAdresse(
                    type: AdresseType,
                    adresseLinje1: String? = null,
                    adresseLinje2: String? = null,
                    adresseLinje3: String? = null,
                    postnr: String? = null,
                    poststed: String? = null,
                    land: String? = null,
                ) = Adresse(
                    aktiv = it == aktivOppholdsadresse,
                    type = type,
                    coAdresseNavn = it.coAdressenavn,
                    adresseLinje1 = adresseLinje1,
                    adresseLinje2 = adresseLinje2,
                    adresseLinje3 = adresseLinje3,
                    postnr = postnr,
                    poststed = poststed,
                    land = land,
                    kilde = it.metadata.master,
                    gyldigFraOgMed = it.gyldigFraOgMed,
                    gyldigTilOgMed = it.gyldigTilOgMed,
                )

                when {
                    it.vegadresse != null -> {
                        toAdresse(
                            type = AdresseType.VEGADRESSE,
                            adresseLinje1 = vegAdresseAsAdresseLinje(it.vegadresse),
                            postnr = it.vegadresse.postnummer,
                        )
                    }

                    it.utenlandskAdresse != null -> {
                        toAdresse(
                            type = AdresseType.UTENLANDSKADRESSE,
                            adresseLinje1 = it.utenlandskAdresse.adressenavnNummer,
                            adresseLinje2 =
                                listOfNotNull(
                                    it.utenlandskAdresse.bygningEtasjeLeilighet,
                                    it.utenlandskAdresse.postboksNummerNavn,
                                ).joinToString(" "),
                            postnr = it.utenlandskAdresse.postkode,
                            poststed = it.utenlandskAdresse.bySted,
                            land = it.utenlandskAdresse.landkode,
                        )
                    }

                    it.oppholdAnnetSted != null -> {
                        toAdresse(
                            type = AdresseType.OPPHOLD_ANNET_STED,
                            adresseLinje1 = it.oppholdAnnetSted,
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
                            type = AdresseType.UKJENT,
                        )
                    }
                }
            }
        }

    fun mapKontaktadresse(
        ppsKlient: ParallelleSannheterKlient,
        kontaktadresse: List<PdlKontaktadresse>,
    ): List<Adresse> =
        runBlocking {
            val aktivOppholdsadresse =
                kontaktadresse
                    .filterNot { it.metadata.historisk }
                    .let { ppsKlient.avklarKontaktadresse(it) }

            kontaktadresse.map {
                fun toAdresse(
                    type: AdresseType,
                    adresseLinje1: String? = null,
                    adresseLinje2: String? = null,
                    adresseLinje3: String? = null,
                    postnr: String? = null,
                    poststed: String? = null,
                    land: String? = null,
                ) = Adresse(
                    aktiv = it == aktivOppholdsadresse,
                    type = type,
                    coAdresseNavn = it.coAdressenavn,
                    adresseLinje1 = adresseLinje1,
                    adresseLinje2 = adresseLinje2,
                    adresseLinje3 = adresseLinje3,
                    postnr = postnr,
                    poststed = poststed,
                    land = land,
                    kilde = it.metadata.master,
                    gyldigFraOgMed = it.gyldigFraOgMed,
                    gyldigTilOgMed = it.gyldigTilOgMed,
                )

                when {
                    it.vegadresse != null -> {
                        toAdresse(
                            type = AdresseType.VEGADRESSE,
                            adresseLinje1 = vegAdresseAsAdresseLinje(it.vegadresse),
                            postnr = it.vegadresse.postnummer,
                        )
                    }

                    it.utenlandskAdresse != null -> {
                        toAdresse(
                            type = AdresseType.UTENLANDSKADRESSE,
                            adresseLinje1 = it.utenlandskAdresse.adressenavnNummer,
                            adresseLinje2 =
                                listOfNotNull(
                                    it.utenlandskAdresse.bygningEtasjeLeilighet,
                                    it.utenlandskAdresse.postboksNummerNavn,
                                ).joinToString(" "),
                            postnr = it.utenlandskAdresse.postkode,
                            poststed = it.utenlandskAdresse.bySted,
                            land = it.utenlandskAdresse.landkode,
                        )
                    }

                    it.postboksadresse != null -> {
                        toAdresse(
                            type = AdresseType.POSTBOKSADRESSE,
                            adresseLinje1 = it.postboksadresse.postbokseier,
                            adresseLinje2 = it.postboksadresse.postboks,
                            postnr = it.postboksadresse.postnummer,
                        )
                    }

                    it.postadresseIFrittFormat != null -> {
                        toAdresse(
                            type = AdresseType.POSTADRESSEFRITTFORMAT,
                            adresseLinje1 = it.postadresseIFrittFormat.adresselinje1,
                            adresseLinje2 = it.postadresseIFrittFormat.adresselinje2,
                            adresseLinje3 = it.postadresseIFrittFormat.adresselinje3,
                            postnr = it.postadresseIFrittFormat.postnummer,
                        )
                    }

                    it.utenlandskAdresseIFrittFormat != null -> {
                        toAdresse(
                            type = AdresseType.UTENLANDSKADRESSEFRITTFORMAT,
                            adresseLinje1 = it.utenlandskAdresseIFrittFormat.adresselinje1,
                            adresseLinje2 = it.utenlandskAdresseIFrittFormat.adresselinje2,
                            adresseLinje3 = it.utenlandskAdresseIFrittFormat.adresselinje3,
                            postnr = it.utenlandskAdresseIFrittFormat.postkode,
                            poststed = it.utenlandskAdresseIFrittFormat.byEllerStedsnavn,
                            land = it.utenlandskAdresseIFrittFormat.landkode,
                        )
                    }

                    else -> {
                        toAdresse(
                            type = AdresseType.UKJENT,
                        )
                    }
                }
            }
        }

    private fun vegAdresseAsAdresseLinje(vegadresse: PdlVegadresse) =
        listOfNotNull(
            vegadresse.adressenavn,
            vegadresse.husnummer,
            vegadresse.husbokstav,
        ).joinToString(" ")
}
