package dolly

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Bruker(val brukerId: String, val brukerType: String, val navIdent: String, val epost: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Gruppe(val id: Long, val navn: String, val hensikt: String)

data class OpprettGruppeRequest(val navn: String, val hensikt: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BestillingStatus(val id: Long, val ferdig: Boolean)