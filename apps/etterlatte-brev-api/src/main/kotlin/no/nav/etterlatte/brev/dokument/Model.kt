package no.nav.etterlatte.brev.dokument

import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.brev.journalpost.BrukerIdType

data class HentJournalposterResult(
    val journalposter: List<Journalpost> = emptyList(),
    val error: Error? = null,
) {
    data class Error(
        val statusCode: HttpStatusCode = HttpStatusCode.InternalServerError,
        val message: String,
    )
}

data class JournalpostResponse(
    val data: DokumentoversiktBruker? = null,
    val errors: List<JournalpostResponseError>? = null,
)

data class DokumentoversiktBruker(
    val dokumentoversiktBruker: Journalposter,
)

data class Journalposter(
    val journalposter: List<Journalpost>,
)

data class JournalpostResponseError(
    val message: String?,
    val locations: List<PdlErrorLocation>? = null,
    val path: List<String>? = null,
    val extensions: PdlErrorExtension? = null,
)

data class PdlErrorLocation(
    val line: String?,
    val column: String?,
)

data class PdlErrorExtension(
    val code: String?,
    val details: String?,
    val classification: String?,
)

data class GraphqlRequest(
    val query: String,
    val variables: DokumentOversiktBrukerVariables,
)

data class DokumentOversiktBrukerVariables(
    val brukerId: BrukerId,
    val foerste: Int,
)

data class BrukerId(
    val id: String,
    val type: BrukerIdType,
)

data class Journalpost(
    val journalpostId: String,
    val tittel: String,
    val journalposttype: String,
    val journalstatus: String,
    val dokumenter: List<Dokumenter>,
    val avsenderMottaker: AvsenderMottaker,
    val kanal: String,
    val datoOpprettet: String,
)

data class Dokumenter(
    val dokumentInfoId: String,
    val tittel: String,
    val dokumentvarianter: List<Dokumentvarianter>,
)

data class Dokumentvarianter(
    val saksbehandlerHarTilgang: Boolean,
)

data class AvsenderMottaker(
    val id: String?,
    val navn: String?,
    val erLikBruker: Boolean?,
)
