package no.nav.etterlatte.libs.common.journalpost

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class JournalpostResponse(
    val journalpostId: String,
    val journalpoststatus: String? = null,
    val melding: String? = null,
    val journalpostferdigstilt: Boolean,
    val dokumenter: List<DokarkivDokument> = emptyList()
)

data class DokarkivDokument(
    val dokumentInfoId: String
)