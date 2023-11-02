package no.nav.etterlatte.joarkhendelser.joark

import io.ktor.http.HttpStatusCode

data class HentJournalpostResult(
    val journalpost: Journalpost? = null,
    val error: Error? = null,
)

data class Error(
    val statusCode: HttpStatusCode = HttpStatusCode.InternalServerError,
    val message: String,
)

data class JournalpostResponse(
    val data: ResponseData? = null,
    val errors: List<JournalpostResponseError>? = null,
) {
    data class ResponseData(
        val journalpost: Journalpost? = null,
    )
}

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
    val variables: JournalpostVariables,
)

data class JournalpostVariables(
    val journalpostId: Long,
)

data class Journalpost(
    val journalpostId: String,
    val bruker: Bruker?,
    val tittel: String,
    val journalposttype: String,
    val journalstatus: Journalstatus,
    val dokument: List<Dokument>,
    val avsenderMottaker: AvsenderMottaker,
    val kanal: String,
    val datoOpprettet: String,
) {
    fun erFerdigstilt(): Boolean = journalstatus == Journalstatus.FERDIGSTILT
}

enum class Journalstatus {
    MOTTATT,
    JOURNALFOERT,
    FERDIGSTILT,
    EKSPEDERT,
    UNDER_ARBEID,
    FEILREGISTRERT,
    UTGAAR,
    AVBRUTT,
    UKJENT_BRUKER,
    RESERVERT,
    OPPLASTING_DOKUMENT,
    UKJENT,
}

data class Bruker(
    val id: String,
    val type: BrukerIdType,
)

enum class BrukerIdType {
    AKTOERID,
    FNR,
    ORGNR,
}

data class Dokument(
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
