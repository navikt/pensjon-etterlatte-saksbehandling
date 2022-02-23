package no.nav.etterlatte.person.pdl

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

data class PdlGraphqlRequest(
    val query: String,
    val variables: PdlVariables
)

/**
 * Tar imot ident(er) som variabel til et [PdlGraphqlRequest].
 *
 * Ident kan være i form av FOLKEREGISTERIDENT (fødselsnummer), AKTORID eller NPID.
 */
//TODO burde ident vært Foedselsnummer?
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlVariables(
    val ident: String? = null,
    val identer: List<String>? = null,
    val historikk: Boolean = false,
    val adresse: Boolean = false,
    val utland: Boolean = false,
    val familieRelasjon: Boolean = false
)
