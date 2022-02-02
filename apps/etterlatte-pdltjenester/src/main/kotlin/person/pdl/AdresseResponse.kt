package person.pdl.adresse

import no.nav.etterlatte.libs.common.pdl.ResponseError
import java.time.LocalDateTime

data class AdresseResponse(
        val `data`: AdresseResponseData,
        val errors: List<ResponseError>? = null
)

data class AdresseResponseData(
        val hentPerson: HentPerson
)
data class HentPerson(
        val adressebeskyttelse: List<Adressebeskyttelse>,
        val bostedsadresse: List<Bostedsadresse>,
        val kontaktadresse: List<Kontaktadresse>,
        val oppholdsadresse: List<Oppholdsadresse>
)
data class Adressebeskyttelse(
        val gradering: String
)
data class Bostedsadresse(
        val gyldigFraOgMed: LocalDateTime? = null,
        val gyldigTilOgMed: LocalDateTime? = null,
        val matrikkeladresse: Matrikkeladresse?,
        val metadata: Metadata,
        val ukjentBosted: UkjentBosted?,
        val utenlandskAdresse: UtenlandskAdresse?,
        val vegadresse: Vegadresse?
)
data class Kontaktadresse(
        val coAdressenavn: String?,
        val folkeregistermetadata: Folkeregistermetadata?,
        val gyldigFraOgMed: LocalDateTime? = null,
        val gyldigTilOgMed: LocalDateTime? = null,
        val metadata: Metadata,
        val postadresseIFrittFormat: PostadresseIFrittFormat?,
        val postboksadresse: Postboksadresse?,
        val type: String,
        val utenlandskAdresse: UtenlandskAdresse?,
        val utenlandskAdresseIFrittFormat: UtenlandskAdresseIFrittFormat?,
        val vegadresse: Vegadresse?
)
data class Oppholdsadresse(
        val coAdressenavn: String?,
        val folkeregistermetadata: Folkeregistermetadata?,
        val gyldigFraOgMed: LocalDateTime? = null,
        val gyldigTilOgMed: LocalDateTime? = null,
        val matrikkeladresse: Matrikkeladresse?,
        val metadata: Metadata,
        val oppholdAnnetSted: String?,
        val utenlandskAdresse: UtenlandskAdresse?,
        val vegadresse: Vegadresse?
)
data class Endring(
        val kilde: String,
        val registrert: LocalDateTime,
        val registrertAv: String,
        val systemkilde: String,
        val type: String
)
data class Postboksadresse (
        val postboks: String,
        val postbokseier: String?,
        val postnummer: String?
)
data class Folkeregistermetadata(
        val aarsak: String?,
        val ajourholdstidspunkt: LocalDateTime? = null,
        val gyldighetstidspunkt: LocalDateTime? = null,
        val kilde: String?,
        val opphoerstidspunkt: LocalDateTime? = null,
        val sekvens: Int?
)

data class Metadata(
        val endringer: List<Endring>,
        val historisk: Boolean,
        val master: String,
        val opplysningsId: String?
)
data class UtenlandskAdresseIFrittFormat (
        val adresselinje1: String?,
        val adresselinje2: String?,
        val adresselinje3: String?,
        val byEllerStedsnavn: String?,
        val landkode: String,
        val postkode: String?,
)
data class PostadresseIFrittFormat (
        val adresselinje1: String?,
        val adresselinje2: String?,
        val adresselinje3: String?,
        val postnummer: String?
)

data class Vegadresse (
        val adressenavn: String?,
        val bruksenhetsnummer: String?,
        val bydelsnummer: String?,
        val husbokstav: String?,
        val husnummer: String?,
        val kommunenummer: String?,
        val koordinater: Koordinater?,
        val matrikkelId: Long?,
        val postnummer: String?,
        val tilleggsnavn: String?,
)
data class UkjentBosted (
        val bostedskommune: String?
)
data class Matrikkeladresse (
        val bruksenhetsnummer: String?,
        val kommunenummer: String?,
        val koordinater: Koordinater?,
        val matrikkelId: Long?,
        val postnummer: String?,
        val tilleggsnavn: String?
)

data class UtenlandskAdresse (
        val adressenavnNummer: String?,
        val bySted: String?,
        val bygningEtasjeLeilighet: String?,
        val landkode: String,
        val postboksNummerNavn: String?,
        val postkode: String?,
        val regionDistriktOmraade: String?
)

data class Koordinater (
        val kvalitet: Int?,
        val x: Float?,
        val y: Float?,
        val z: Float?
)