package no.nav.etterlatte.libs.common.pdl

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

data class GraphqlRequest(
    val query: String,
    val variables: Variables
)

/**
 * Tar imot ident(er) som variabel til et [GraphqlRequest].
 *
 * Ident kan være i form av FOLKEREGISTERIDENT (fødselsnummer), AKTORID eller NPID.
 */
//TODO burde ident vært Foedselsnummer?
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Variables(
    val ident: String? = null,
    val identer: List<String>? = null,
    val historikk: Boolean = false,
    val adresse: Boolean = false,
    val utland: Boolean = false,
    val familieRelasjon: Boolean = false
)
