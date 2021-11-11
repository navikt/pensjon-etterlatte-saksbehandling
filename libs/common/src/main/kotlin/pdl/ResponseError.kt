package no.nav.etterlatte.libs.common.pdl

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * PDL-spesifikke feil fra GraphQL.
 *
 * @see: <a href="https://navikt.github.io/pdl/#_graphql_feilh%C3%A5ndtering_ved_konsumering_av_pdl_api">Appendix D: GraphQL: Feilhåndtering ved konsumering av Pdl-Api</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ResponseError(
    val message: String?,
    val locations: List<ErrorLocation>? = null,
    val path: List<String>? = null,
    val extensions: ErrorExtension? = null
)

data class ErrorLocation(
    val line: String?,
    val column: String?
)

data class ErrorExtension(
    val code: String?,
    val details: ErrorDetails?,
    val classification: String?
)

data class ErrorDetails(
    val type: String? = null,
    val cause: String? = null,
    val policy: String? = null
)
