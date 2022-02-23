package no.nav.etterlatte.person.pdl

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * PDL-spesifikke feil fra GraphQL.
 *
 * @see: <a href="https://navikt.github.io/pdl/#_graphql_feilh%C3%A5ndtering_ved_konsumering_av_pdl_api">Appendix D: GraphQL: Feilhåndtering ved konsumering av Pdl-Api</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
