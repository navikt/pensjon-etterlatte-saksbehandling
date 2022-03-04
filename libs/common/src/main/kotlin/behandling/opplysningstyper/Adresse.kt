package no.nav.etterlatte.libs.common.behandling.opplysningstyper

import no.nav.etterlatte.libs.common.person.AdresseType
import java.time.LocalDateTime


data class Adresse(
    val type: AdresseType,
    val aktiv: Boolean,
    val coAdresseNavn: String?,
    val adresseLinje1: String?,
    val adresseLinje2: String?,
    val adresseLinje3: String?,
    val postnr: String?,
    val poststed: String?,
    val land: String?,
    val kilde: String,
    val gyldigFraOgMed: LocalDateTime?,
    val gyldigTilOgMed: LocalDateTime?,
)

data class Bostedadresse(
    val bostedadresse: List<Adresse>?
)


