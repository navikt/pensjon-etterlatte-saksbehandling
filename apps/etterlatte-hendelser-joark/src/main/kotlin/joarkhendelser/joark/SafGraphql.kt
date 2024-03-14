package no.nav.etterlatte.joarkhendelser.joark

import com.fasterxml.jackson.annotation.JsonCreator
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem

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
    val tittel: String?,
    val journalposttype: String,
    val journalstatus: Journalstatus,
    val dokumenter: List<Dokument>,
    val sak: Fagsak?,
    val avsenderMottaker: AvsenderMottaker,
    val kanal: Kanal,
    val datoOpprettet: String,
    val opprettetAvNavn: String?,
) {
    fun erFerdigstilt(): Boolean =
        (journalstatus == Journalstatus.FERDIGSTILT || journalstatus == Journalstatus.JOURNALFOERT) &&
            sak?.fagsaksystem == Fagsaksystem.EY.navn
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
    val tittel: String? = null,
    val dokumentvarianter: List<Dokumentvarianter>,
)

data class Dokumentvarianter(
    val saksbehandlerHarTilgang: Boolean,
)

data class Fagsak(
    val fagsakId: String?,
    val fagsaksystem: String?,
    val sakstype: String?,
    val tema: String?,
)

data class AvsenderMottaker(
    val id: String?,
    val navn: String?,
    val erLikBruker: Boolean?,
)

enum class Kanal(val beskrivelse: String) {
    ALTINN("Altinn"),
    EESSI("EESSI"),
    EIA("EIA"),
    EKST_OPPS("Ekstern kilde"),
    LOKAL_UTSKRIFT("Lokal utskrift"),
    NAV_NO("nav.no"),
    SENTRAL_UTSKRIFT("Sentral utskrift"),
    SDP("SDP"),
    SKAN_NETS("Skanning - NETS"),
    SKAN_PEN("Skanning - Pensjon"),
    SKAN_IM("Skanning - Iron Moutain"),
    TRYGDERETTEN("Trygderetten"),
    HELSENETTET("Helsenettet"),
    INGEN_DISTRIBUSJON("Ingen distribusjon"),
    NAV_NO_UINNLOGGET("Uinnlogget (nav.no)"),
    INNSENDT_NAV_ANSATT("Innsendt av Nav-ansatt"),
    NAV_NO_CHAT("Chat (nav.no)"),
    DPVT("DPVT"),
    UKJENT("Ukjent"),
    ;

    companion object {
        @JvmStatic
        @JsonCreator
        fun fraVerdi(kanal: String) = entries.firstOrNull { it.name == kanal } ?: UKJENT
    }
}
