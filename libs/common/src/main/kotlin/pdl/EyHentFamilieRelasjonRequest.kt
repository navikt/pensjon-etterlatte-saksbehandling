package no.nav.etterlatte.libs.common.pdl

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.etterlatte.libs.common.person.Foedselsnummer

/**
 * Tar imot ident(er) som variabel til et [GraphqlRequest].
 *
 * Ident kan være i form av FOLKEREGISTERIDENT (fødselsnummer), AKTORID eller NPID.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class EyHentFamilieRelasjonRequest(
    val fnr: Foedselsnummer,
    val historikk: Boolean = false
)
