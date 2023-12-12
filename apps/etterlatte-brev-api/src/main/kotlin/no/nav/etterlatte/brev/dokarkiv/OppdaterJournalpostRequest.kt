package no.nav.etterlatte.brev.dokarkiv

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Requestobjekt for å oppdatere eksisterende Journalpost.
 * Setter [JsonInclude.Include.NON_EMPTY] siden Dokakiv ignorerer verdier som mangler eller er null.
 **/
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class OppdaterJournalpostRequest(
    val avsenderMottaker: AvsenderMottaker?,
    val bruker: Bruker?,
    val dokumenter: List<DokumentInfo>?,
    val sak: JournalpostSak?,
    val tema: String?,
    val tittel: String?,
) {
    data class Bruker(
        val id: String,
        @JsonProperty("idType")
        @JsonAlias("type")
        val idType: BrukerIdType,
    )

    data class DokumentInfo(
        val dokumentInfoId: String,
        val tittel: String,
    )

    data class AvsenderMottaker(
        val id: String?,
        @JsonProperty("idType")
        @JsonAlias("type")
        val idType: String? = null,
        val navn: String? = null,
    )
}
