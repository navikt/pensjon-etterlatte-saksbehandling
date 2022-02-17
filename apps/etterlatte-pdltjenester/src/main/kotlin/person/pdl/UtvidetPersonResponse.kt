package no.nav.etterlatte.person.pdl

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.libs.common.pdl.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdl.ResponseError
import java.time.LocalDate
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class UtvidetPersonResponse(
    val data: UtvidetPersonResponseData? = null,
    val errors: List<ResponseError>? = null
)

data class UtvidetPersonResponseData(
    val hentPerson: HentUtvidetPerson? = null
)

data class HentUtvidetPerson(
    val adressebeskyttelse: List<Adressebeskyttelse>,
    val navn: List<Navn>,
    val foedsel: List<Foedsel>,
    val sivilstand: List<Sivilstand>,
    val doedsfall: List<Doedsfall>,
    val bostedsadresse: List<Bostedsadresse>?,
    val kontaktadresse: List<Kontaktadresse>?,
    val oppholdsadresse: List<Oppholdsadresse>?,
    val innflyttingTilNorge: List<InnflyttingTilNorge>?,
    val statsborgerskap: List<Statsborgerskap>?,
    val utflyttingFraNorge: List<UtflyttingFraNorge>?,
    val foreldreansvar: List<ForelderAnsvar>?,
    val forelderBarnRelasjon: List<ForelderBarnRelasjon>?
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
    val forkortetNavn: String? = null,
    val gyldigFraOgMed: LocalDate? = null,
    val folkeregistermetadata: Folkeregistermetadata? = null,
    val metadata: Metadata
)

data class Statsborgerskap(
    val land: String,
    val gyldigFraOgMed: LocalDate? = null,
    val gyldigTilOgMed: LocalDate? = null,
    val metadata: Metadata
)

data class Foedsel(
    val foedselsdato: LocalDate? = null,
    val foedeland: String? = null,
    val foedested: String? = null,
    val foedselsaar: Int? = null,
    val folkeregistermetadata: Folkeregistermetadata? = null,
    val metadata: Metadata
)

data class Folkeregistermetadata(
    val gyldighetstidspunkt: LocalDateTime? = null
)

data class Metadata(
    val endringer: List<Endring>,
    val historisk: Boolean,
    val master: String,
    val opplysningsId: String
) {
    fun sisteRegistrertDato(): LocalDateTime {
        return endringer.maxByOrNull { it.registrert }?.registrert!!
    }
}

data class Endring(
    val kilde: String?,
    val registrert: LocalDateTime,
    val registrertAv: String?,
    val systemkilde: String?,
    val type: Endringstype
)

enum class Endringstype {
    KORRIGER,
    OPPHOER,
    OPPRETT;
}

enum class Sivilstandstype {
    UOPPGITT,
    UGIFT,
    GIFT,
    ENKE_ELLER_ENKEMANN,
    SKILT,
    SEPARERT,
    REGISTRERT_PARTNER,
    SEPARERT_PARTNER,
    SKILT_PARTNER,
    GJENLEVENDE_PARTNER;
}

data class Sivilstand(
    val type: Sivilstandstype,
    val gyldigFraOgMed: LocalDate? = null,
    val relatertVedSivilstand: String? = null,
    val metadata: Metadata
)

data class Doedsfall(
    val doedsdato: LocalDate?,
    val folkeregistermetadata: Folkeregistermetadata?,
    val metadata: Metadata
)

data class InnflyttingTilNorge(
    val folkeregistermetadata: Folkeregistermetadata?,
    val fraflyttingsland: String?,
    val fraflyttingsstedIUtlandet: String?,
    val metadata: Metadata
)
//TODO endre til Date?
data class UtflyttingFraNorge(
    val folkeregistermetadata: Folkeregistermetadata?,
    val tilflyttingsland: String?,
    val tilflyttingsstedIUtlandet: String?,
    val utflyttingsdato: String?,
    val metadata: Metadata
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

data class Postboksadresse (
    val postboks: String,
    val postbokseier: String?,
    val postnummer: String?
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

data class ForelderAnsvar(
    val ansvar: String? = null,
    val ansvarlig: String? = null,
    val ansvarligUtenIdentifikator: RelatertBiPerson? = null,
    val ansvarssubjekt: String? = null,
    val folkeregistermetadata: no.nav.etterlatte.person.pdl.Folkeregistermetadata? = null,
    val metadata: no.nav.etterlatte.person.pdl.Metadata,
)

data class RelatertBiPerson(
    val foedselsdato: LocalDate? = null,
    val kjoenn: String? = null,
    val navn: Personnavn? = null,
    val statsborgerskap: String? = null
)

data class Personnavn(
    val etternavn: String,
    val fornavn: String,
    val mellomnavn: String? = null
)

data class ForelderBarnRelasjon(
    val folkeregistermetadata: no.nav.etterlatte.person.pdl.Folkeregistermetadata? = null,
    val metadata: no.nav.etterlatte.person.pdl.Metadata,
    val minRolleForPerson: ForelderBarnRelasjonRolle? = null,
    val relatertPersonsIdent: String,
    val relatertPersonsRolle: ForelderBarnRelasjonRolle
)

enum class ForelderBarnRelasjonRolle {
    BARN,
    FAR,
    MEDMOR,
    MOR
}