package journalpost

import no.nav.etterlatte.libs.common.journalpost.BrukerIdType

interface JournalpostService {
    suspend fun hentInnkommendeBrev(fnr: String, idType: BrukerIdType, accessToken: String): JournalpostResponse

    suspend fun hentInnkommendeBrevInnhold(
        journalpostId: String,
        dokumentInfoId: String,
        accessToken: String
    ): ByteArray
}

data class Journalpost(
    val journalpostId: String,
    val tittel: String,
    val journalposttype: String,
    val journalstatus: String,
    val dokumenter: List<Dokumenter>,
    val avsenderMottaker: AvsenderMottaker,
    val kanal: String,
    val datoOpprettet: String // Mulig Dato?
)

data class Dokumenter(
    val dokumentInfoId: String,
    val tittel: String,
    val dokumentvarianter: List<Dokumentvarianter>
)

data class Dokumentvarianter(
    val saksbehandlerHarTilgang: Boolean
)

data class AvsenderMottaker(
    val id: String,
    val navn: String,
    val erLikBruker: Boolean
)