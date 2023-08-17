package no.nav.etterlatte.pdl

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import java.time.LocalDate
import java.time.LocalDateTime

data class PdlGraphqlRequest(
    val query: String,
    val variables: PdlVariables
)

data class PdlGraphqlBolkRequest(
    val query: String,
    val variables: PdlBolkVariables
)

data class PdlFolkeregisterIdentRequest(
    val query: String,
    val variables: PdlFolkeregisterIdentVariables
)

data class PdlVariables(
    val ident: String,
    val bostedsadresse: Boolean,
    val bostedsadresseHistorikk: Boolean,
    val deltBostedsadresse: Boolean,
    val oppholdsadresse: Boolean,
    val oppholdsadresseHistorikk: Boolean,
    val kontaktadresse: Boolean,
    val kontaktadresseHistorikk: Boolean,
    val utland: Boolean,
    val sivilstand: Boolean,
    val familieRelasjon: Boolean,
    val vergemaal: Boolean
)

data class PdlBolkVariables(
    val identer: List<String>,
    val bostedsadresse: Boolean,
    val bostedsadresseHistorikk: Boolean,
    val deltBostedsadresse: Boolean,
    val oppholdsadresse: Boolean,
    val oppholdsadresseHistorikk: Boolean,
    val kontaktadresse: Boolean,
    val kontaktadresseHistorikk: Boolean,
    val utland: Boolean,
    val sivilstand: Boolean,
    val familieRelasjon: Boolean,
    val vergemaal: Boolean
)

data class PdlFolkeregisterIdentVariables(
    val ident: String,
    val grupper: List<String>,
    val historikk: Boolean
)

data class PdlPersonResponse(
    val data: PdlPersonResponseData? = null,
    val errors: List<PdlResponseError>? = null
)

data class PdlPersonResponseBolk(
    val data: PdlPersonResponseBulkData? = null,
    val errors: List<PdlResponseError>? = null
)

data class PdlIdentResponse(
    val data: PdlFolkegisterIdentData? = null,
    val errors: List<PdlResponseError>? = null
)

data class PdlFolkegisterIdentData(
    val hentIdenter: PdlFolkeregisterIdentResult? = null
)

data class PdlFolkeregisterIdentResult(
    val identer: List<PdlIdenter>
)

data class PdlIdenter(
    val ident: String,
    val historisk: Boolean,
    val gruppe: String
)

data class PdlResponseError(
    val message: String?,
    val locations: List<PdlErrorLocation>? = null,
    val path: List<String>? = null,
    val extensions: PdlErrorExtension? = null
)

data class PdlErrorLocation(
    val line: String?,
    val column: String?
)

data class PdlErrorExtension(
    val code: String?,
    val details: PdlErrorDetails?,
    val classification: String?
)

data class PdlErrorDetails(
    val type: String? = null,
    val cause: String? = null,
    val policy: String? = null
)

data class PdlPersonResponseData(
    val hentPerson: PdlHentPerson? = null
)

data class PdlPersonResponseBulkData(
    val hentPersonBolk: List<PdlHentPersonBolkResult>? = null
)

data class PdlHentPersonBolkResult(
    val code: String,
    val ident: String,
    val person: PdlHentPerson? = null
)

data class PdlHentPerson(
    val adressebeskyttelse: List<PdlAdressebeskyttelse>,
    val navn: List<PdlNavn>,
    val foedsel: List<PdlFoedsel>,
    val sivilstand: List<PdlSivilstand>?,
    val doedsfall: List<PdlDoedsfall>,
    val bostedsadresse: List<PdlBostedsadresse>?,
    val deltBostedsadresse: List<PdlDeltBostedsadresse>?,
    val kontaktadresse: List<PdlKontaktadresse>?,
    val oppholdsadresse: List<PdlOppholdsadresse>?,
    val innflyttingTilNorge: List<PdlInnflyttingTilNorge>?,
    val statsborgerskap: List<PdlStatsborgerskap>?,
    val utflyttingFraNorge: List<PdlUtflyttingFraNorge>?,
    val foreldreansvar: List<PdlForelderAnsvar>?,
    val forelderBarnRelasjon: List<PdlForelderBarnRelasjon>?,
    var vergemaalEllerFremtidsfullmakt: List<PdlVergemaalEllerFremtidsfullmakt>?

)

data class PdlAdressebeskyttelse(
    val gradering: PdlGradering?,
    val folkeregistermetadata: PdlFolkeregistermetadata? = null,
    val metadata: PdlMetadata
)

enum class PdlGradering {
    STRENGT_FORTROLIG_UTLAND,
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT
}

data class PdlNavn(
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
    val forkortetNavn: String? = null,
    val gyldigFraOgMed: LocalDate? = null,
    val folkeregistermetadata: PdlFolkeregistermetadata? = null,
    val metadata: PdlMetadata
)

data class PdlStatsborgerskap(
    val land: String,
    val gyldigFraOgMed: LocalDate? = null,
    val gyldigTilOgMed: LocalDate? = null,
    val metadata: PdlMetadata
)

data class PdlFoedsel(
    val foedselsdato: LocalDate? = null,
    val foedeland: String? = null,
    val foedested: String? = null,
    val foedselsaar: Int,
    val folkeregistermetadata: PdlFolkeregistermetadata? = null,
    val metadata: PdlMetadata
)

data class PdlFolkeregistermetadata(
    val gyldighetstidspunkt: LocalDateTime? = null,
    val opphoerstidspunkt: LocalDateTime? = null
)

data class PdlMetadata(
    val endringer: List<PdlEndring>,
    val historisk: Boolean,
    val master: String,
    val opplysningsId: String
) {
    fun sisteRegistrertDato(): LocalDateTime {
        return endringer.maxByOrNull { it.registrert }?.registrert!!
    }
}

data class PdlEndring(
    val kilde: String?,
    val registrert: LocalDateTime,
    val registrertAv: String?,
    val systemkilde: String?,
    val type: PdlEndringstype
)

enum class PdlEndringstype {
    KORRIGER,
    OPPHOER,
    OPPRETT
}

enum class PdlSivilstandstype {
    UOPPGITT,
    UGIFT,
    GIFT,
    ENKE_ELLER_ENKEMANN,
    SKILT,
    SEPARERT,
    REGISTRERT_PARTNER,
    SEPARERT_PARTNER,
    SKILT_PARTNER,
    GJENLEVENDE_PARTNER
}

data class PdlSivilstand(
    val type: PdlSivilstandstype,
    val gyldigFraOgMed: LocalDate? = null,
    val relatertVedSivilstand: String? = null,
    val bekreftelsesdato: LocalDate? = null,
    val metadata: PdlMetadata
)

data class PdlDoedsfall(
    val doedsdato: LocalDate?,
    val folkeregistermetadata: PdlFolkeregistermetadata?,
    val metadata: PdlMetadata
)

data class PdlInnflyttingTilNorge(
    val folkeregistermetadata: PdlFolkeregistermetadata?,
    val fraflyttingsland: String?,
    val fraflyttingsstedIUtlandet: String?,
    val metadata: PdlMetadata
)

data class PdlUtflyttingFraNorge(
    val folkeregistermetadata: PdlFolkeregistermetadata?,
    val tilflyttingsland: String?,
    val tilflyttingsstedIUtlandet: String?,
    val utflyttingsdato: LocalDate?,
    val metadata: PdlMetadata
)

data class PdlBostedsadresse(
    val coAdressenavn: String?,
    val gyldigFraOgMed: LocalDateTime? = null,
    val gyldigTilOgMed: LocalDateTime? = null,
    val matrikkeladresse: PdlMatrikkeladresse?,
    val metadata: PdlMetadata,
    val folkeregistermetadata: PdlFolkeregistermetadata?,
    val ukjentBosted: PdlUkjentBosted?,
    val utenlandskAdresse: PdlUtenlandskAdresse?,
    val vegadresse: PdlVegadresse?
)

data class PdlDeltBostedsadresse(
    val coAdressenavn: String?,
    val startdatoForKontrakt: LocalDateTime? = null,
    val sluttdatoForKontrakt: LocalDateTime? = null,
    val matrikkeladresse: PdlMatrikkeladresse?,
    val metadata: PdlMetadata,
    val folkeregistermetadata: PdlFolkeregistermetadata?,
    val ukjentBosted: PdlUkjentBosted?,
    val utenlandskAdresse: PdlUtenlandskAdresse?,
    val vegadresse: PdlVegadresse?
)

data class PdlKontaktadresse(
    val coAdressenavn: String?,
    val folkeregistermetadata: PdlFolkeregistermetadata?,
    val gyldigFraOgMed: LocalDateTime? = null,
    val gyldigTilOgMed: LocalDateTime? = null,
    val metadata: PdlMetadata,
    val postadresseIFrittFormat: PdlPostadresseIFrittFormat?,
    val postboksadresse: PdlPostboksadresse?,
    val type: String,
    val utenlandskAdresse: PdlUtenlandskAdresse?,
    val utenlandskAdresseIFrittFormat: PdlUtenlandskAdresseIFrittFormat?,
    val vegadresse: PdlVegadresse?
)

data class PdlOppholdsadresse(
    val coAdressenavn: String?,
    val folkeregistermetadata: PdlFolkeregistermetadata?,
    val gyldigFraOgMed: LocalDateTime? = null,
    val gyldigTilOgMed: LocalDateTime? = null,
    val matrikkeladresse: PdlMatrikkeladresse?,
    val metadata: PdlMetadata,
    val oppholdAnnetSted: String?,
    val utenlandskAdresse: PdlUtenlandskAdresse?,
    val vegadresse: PdlVegadresse?
)

data class PdlPostboksadresse(
    val postboks: String,
    val postbokseier: String?,
    val postnummer: String?
)

data class PdlUtenlandskAdresseIFrittFormat(
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val byEllerStedsnavn: String?,
    val landkode: String,
    val postkode: String?
)

data class PdlPostadresseIFrittFormat(
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val postnummer: String?
)

data class PdlVegadresse(
    val adressenavn: String?,
    val bruksenhetsnummer: String?,
    val bydelsnummer: String?,
    val husbokstav: String?,
    val husnummer: String?,
    val kommunenummer: String?,
    val koordinater: PdlKoordinater?,
    val matrikkelId: Long?,
    val postnummer: String?,
    val tilleggsnavn: String?
)

data class PdlUkjentBosted(
    val bostedskommune: String?
)

data class PdlMatrikkeladresse(
    val bruksenhetsnummer: String?,
    val kommunenummer: String?,
    val koordinater: PdlKoordinater?,
    val matrikkelId: Long?,
    val postnummer: String?,
    val tilleggsnavn: String?
)

data class PdlUtenlandskAdresse(
    val adressenavnNummer: String?,
    val bySted: String?,
    val bygningEtasjeLeilighet: String?,
    val landkode: String,
    val postboksNummerNavn: String?,
    val postkode: String?,
    val regionDistriktOmraade: String?
)

data class PdlKoordinater(
    val kvalitet: Int?,
    val x: Float?,
    val y: Float?,
    val z: Float?
)

data class PdlForelderAnsvar(
    val ansvar: String? = null,
    val ansvarlig: String? = null,
    val ansvarligUtenIdentifikator: PdlRelatertBiPerson? = null,
    val ansvarssubjekt: String? = null,
    val folkeregistermetadata: PdlFolkeregistermetadata? = null,
    val metadata: PdlMetadata
)

data class PdlRelatertBiPerson(
    val foedselsdato: LocalDate? = null,
    val kjoenn: String? = null,
    val navn: PdlPersonnavn? = null,
    val statsborgerskap: String? = null
)

data class PdlPersonnavn(
    val etternavn: String,
    val fornavn: String,
    val mellomnavn: String? = null
)

data class PdlForelderBarnRelasjon(
    val folkeregistermetadata: PdlFolkeregistermetadata? = null,
    val metadata: PdlMetadata,
    val minRolleForPerson: PdlForelderBarnRelasjonRolle? = null,
    val relatertPersonsIdent: String?,
    val relatertPersonsRolle: PdlForelderBarnRelasjonRolle
)

enum class PdlForelderBarnRelasjonRolle {
    BARN,
    FAR,
    MEDMOR,
    MOR
}

data class PdlVergemaalEllerFremtidsfullmakt(
    val embete: String?,
    val folkeregistermetadata: PdlFolkeregistermetadata? = null,
    val metadata: PdlMetadata,
    val type: String?,
    val vergeEllerFullmektig: PdlVergeEllerFullmektig
)

data class PdlVergeEllerFullmektig(
    val motpartsPersonident: Folkeregisteridentifikator?,
    val navn: PdlPersonnavn?,
    val omfang: String?,
    val omfangetErInnenPersonligOmraade: Boolean
)

data class PdlGeografiskTilknytningIdentVariables(
    val ident: String
)
data class PdlGeografiskTilknytningRequest(
    val query: String,
    val variables: PdlGeografiskTilknytningIdentVariables
)

enum class PdlGtType {
    KOMMUNE, BYDEL, UTLAND, UDEFINERT
}

data class PdlGeografiskTilknytning(
    val gtBydel: String?,
    val gtKommune: String?,
    val gtLand: String?,
    val gtType: PdlGtType?
)

data class PdlGeografiskTilknytningData(
    val hentGeografiskTilknytning: PdlGeografiskTilknytning? = null
)

data class PdlGeografiskTilknytningResponse(
    val data: PdlGeografiskTilknytningData? = null,
    val errors: List<PdlResponseError>? = null
)

data class PdlFoedselsnumreFraAktoerIdRequest(
    val query: String,
    val variables: IdenterBolkVariables
)

data class IdenterBolkVariables(
    val identer: List<String>,
    val grupper: Set<IdentGruppe>
)

data class PdlFoedselsnumreFraAktoerIdResponse(
    val data: PdlFoedselsnumreFraAktoerIdData
)

data class PdlFoedselsnumreFraAktoerIdData(
    val hentIdenterBolk: Set<HentIdenterBolkResult>
)

data class HentIdenterBolkResult(
    val ident: String,
    val identer: Set<IdentInformasjon>,
    val code: String
)

data class IdentInformasjon(
    val ident: String,
    val gruppe: IdentGruppe,
    val historisk: Boolean
)

enum class IdentGruppe {
    AKTORID,
    FOLKEREGISTERIDENT,
    NPID
}

data class PdlHentForelderansvarHistorikkVariables(
    val ident: String
) {
    val historikk: Boolean = true
}

data class PdlHentForeldreansvarHistorikkRequest(
    val query: String,
    val variables: PdlHentForelderansvarHistorikkVariables
)

data class PdlHentForeldreansvarHistorikkResponse(
    val data: PdlHistorikkForeldreansvar? = null,
    val errors: List<PdlResponseError>? = null
)

data class PdlHistorikkForeldreansvar(
    val foreldreansvar: List<PdlForelderAnsvar>,
    val forelderBarnRelasjon: List<PdlForelderBarnRelasjon>
)