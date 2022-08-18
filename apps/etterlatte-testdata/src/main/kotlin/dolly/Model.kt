package dolly

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Bruker(val brukerId: String?, val brukerType: String?, val navIdent: String?, val epost: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Gruppe(val id: Long, val navn: String, val hensikt: String)

data class OpprettGruppeRequest(val navn: String, val hensikt: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BestillingStatus(val id: Long, val ferdig: Boolean)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TestGruppeBestillinger(val identer: List<TestGruppeBestilling>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TestGruppeBestilling(val ident: String, val bestillingId: List<Long>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DollyPersonResponse(val ident: String, val person: DollyPerson)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DollyPerson(
    val doedsfall: List<Doedsfall>,
    val foedsel: List<Foedsel>,
    val navn: List<Navn>,
    val forelderBarnRelasjon: List<ForelderBarnRelasjon>,
    val sivilstand: List<Sivilstand>
)

data class ForelderBarnRelasjon(
    val relatertPersonsIdent: String,
    val barn: Boolean,
)

data class Sivilstand(val type: String, val relatertVedSivilstand: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Doedsfall(val doedsdato: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Foedsel(val foedselsdato: String, val foedselsaar: Int)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Navn(val fornavn: String, val etternavn: String)

data class ForenkletFamilieModell(val avdoed: String, val gjenlevende: String, val barn: List<String>)
