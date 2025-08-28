package no.nav.etterlatte.joarkhendelser.joark

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem

data class JournalpostResponse(
    val data: ResponseData? = null,
    val errors: List<Error>? = null,
) {
    data class ResponseData(
        val journalpost: Journalpost? = null,
    )
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Error(
    val message: String?,
    val path: List<String> = emptyList(),
    val extensions: Extensions?,
) {
    data class Extensions(
        val code: Code?,
        val classification: String?,
    )

    enum class Code {
        FORBIDDEN,
        NOT_FOUND,
        BAD_REQUEST,
        SERVER_ERROR,
        ;

        // SAF sender feilkoder i lowercase
        companion object {
            @JvmStatic
            @JsonCreator
            fun of(value: String?) = entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        }
    }
}

data class GraphqlRequest(
    val query: String,
    val variables: JournalpostVariables,
)

data class JournalpostVariables(
    val journalpostId: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Journalpost(
    val journalpostId: String,
    val bruker: Bruker?,
    val tittel: String?,
    val journalstatus: Journalstatus,
    val journalfoerendeEnhet: String?,
    val dokumenter: List<DokumentInfo>,
    val sak: Fagsak?,
    val kanal: Kanal,
) {
    fun erFerdigstilt(): Boolean =
        (journalstatus == Journalstatus.FERDIGSTILT || journalstatus == Journalstatus.JOURNALFOERT) &&
            sak?.fagsaksystem == Fagsaksystem.EY.navn
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class DokumentInfo(
    val dokumentInfoId: String,
    val tittel: String?,
    val brevkode: String?,
)

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

data class Fagsak(
    val fagsakId: String?,
    val fagsaksystem: String?,
    val sakstype: String?,
    val tema: String?,
)

enum class Kanal(
    val beskrivelse: String,
) {
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
    SKAN_IM("Skanning - Iron Mountain"),
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
