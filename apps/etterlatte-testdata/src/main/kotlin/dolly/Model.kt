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
data class DollyPersonResponse(val id: Long, val person: DollyPerson, val relasjoner: Relasjoner)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DollyPerson(val doedsfall: List<Doedsfall>, val foedsel: List<Foedsel>, val navn: List<Navn>)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Doedsfall(val doedsdato: String)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Foedsel(val foedselsdato: String, val foedselsaar: Int)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Navn(val fornavn: String, val etternavn: String)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Relasjoner(val relasjon: List<Relasjon>)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Relasjon(val relasjonType: String, val relatertPerson: RelatertPerson)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RelatertPerson(val navn: List<Navn>, val ident: String)
