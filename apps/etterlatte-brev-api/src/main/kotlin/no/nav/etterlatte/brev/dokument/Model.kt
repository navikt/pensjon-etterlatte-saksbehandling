package no.nav.etterlatte.brev.dokument

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.brev.dokarkiv.BrukerIdType
import no.nav.etterlatte.brev.dokarkiv.JournalpostSak

data class HentUtsendingsinfoResponse(
    val data: ResponseData? = null,
    val errors: List<Error>? = null,
) {
    data class ResponseData(
        val journalpost: JournalpostUtsendingsinfo? = null,
    )
}

data class JournalpostResponse(
    val data: ResponseData? = null,
    val errors: List<Error>? = null,
) {
    data class ResponseData(
        val journalpost: Journalpost? = null,
    )
}

data class DokumentoversiktBrukerResponse(
    val data: DokumentoversiktBruker? = null,
    val errors: List<Error>? = null,
)

data class DokumentoversiktBruker(
    val dokumentoversiktBruker: Journalposter,
)

data class Journalposter(
    val journalposter: List<Journalpost>,
)

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
    val variables: GraphqlVariables,
)

data class DokumentOversiktBrukerVariables(
    val brukerId: BrukerId,
    val tema: List<String>,
    val foerste: Int,
) : GraphqlVariables

data class JournalpostVariables(
    val journalpostId: String,
) : GraphqlVariables

interface GraphqlVariables

data class BrukerId(
    val id: String,
    val type: BrukerIdType,
)

data class Journalpost(
    val journalpostId: String,
    val tittel: String?,
    val tema: String?,
    val journalposttype: String,
    val journalstatus: Journalstatus,
    val dokumenter: List<DokumentInfo>,
    val avsenderMottaker: AvsenderMottaker,
    val kanal: String?,
    val bruker: Bruker?,
    val sak: JournalpostSak?,
    val datoOpprettet: String,
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

data class JournalpostUtsendingsinfo(
    val journalpostId: String,
    val utsendingsinfo: Utsendingsinfo?,
)

data class DokumentInfo(
    val dokumentInfoId: String,
    val tittel: String?,
    val dokumentvarianter: List<Dokumentvariant>,
)

data class Dokumentvariant(
    val variantformat: Variantformat?,
    val saksbehandlerHarTilgang: Boolean,
)

enum class Variantformat {
    ARKIV,
    ORIGINAL,
    SLADDET,
    FULLVERSJON,
    PRODUKSJON,
    PRODUKSJON_DLF,
}

data class Bruker(
    val id: String,
    val type: BrukerIdType,
)

data class AvsenderMottaker(
    val id: String?,
    val type: String?,
    val navn: String?,
    val land: String?,
    val erLikBruker: Boolean?,
)

// https://confluence.adeo.no/display/BOA/Type%3A+Utsendingsinfo
@JsonIgnoreProperties(ignoreUnknown = true)
data class Utsendingsinfo(
    val fysiskpostSendt: FysiskpostSendt?,
    val digitalpostSendt: DigitalpostSendt?,
) {
    // Mappes hvis Journalpost.utsendingskanal er S (sentral utskrift)
    data class FysiskpostSendt(
        val adressetekstKonvolutt: String?,
    )

    // Mappes hvis Journalpost.utsendingskanal er SDP (sikker digital postkasse)
    data class DigitalpostSendt(
        val adresse: String?,
    )
}
