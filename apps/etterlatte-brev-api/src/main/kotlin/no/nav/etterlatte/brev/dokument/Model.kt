package no.nav.etterlatte.brev.dokument

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.etterlatte.brev.dokarkiv.AvsenderMottakerIdType
import no.nav.etterlatte.brev.dokarkiv.BrukerIdType
import no.nav.etterlatte.brev.dokarkiv.JournalpostSak
import no.nav.etterlatte.libs.common.person.maskerFnr

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
    val sideInfo: SideInfo,
)

/*
* Metadata for å paginere mellom journalposter.
*
* Verdt å merke seg at denne gir utrolig forvirrende og lite intuitive verdier.
* Eks. totaltAntall er ikke totalen du har filtrert på, men totalen som er registrert på bruker.
*
* Pagineringen vil også oppføre seg merkelig siden den også teller med de som er filtrert ut.
* Eks. du søker på tema EYO, foerste=10 (antallet du henter). Du kan da få totaltAntall=30
* i retur, hvorav 10 av de har tema PEN.
* Det kan da se slik ut i bolker på 10:
*   [9 EYO, 1 PEN], [5 EYO, 5 PEN], [6 EYO, 4 PEN]
* I tilfellet over vil da første side kun inneholde 9 journalposter, selv om du har bedt om 10 stk.
* Neste side vil så ha 5 journalposter, og siste side vil ha 6... ikke det minste forvirrende.
*/
data class SideInfo(
    // 	Når man paginerer forover, pekeren for å fortsette.
    val sluttpeker: String? = null,
    // 	True/False verdi for om neste side eksisterer, når man paginerer forover.
    val finnesNesteSide: Boolean,
    // 	Antall journalposter på denne siden.
    val antall: Int,
    // 	Totalt antall journalposter på alle sider.
    val totaltAntall: Int,
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
    val journalposttyper: List<Journalposttype>,
    val journalstatuser: List<Journalstatus>,
    val foerste: Int,
    val etter: String? = null,
) : GraphqlVariables

data class JournalpostVariables(
    val journalpostId: String,
) : GraphqlVariables

interface GraphqlVariables

data class BrukerId(
    val id: String,
    val type: BrukerIdType,
) {
    override fun toString(): String = "BrukerId(id=${id.maskerFnr()}, type=$type)"
}

data class Journalpost(
    val journalpostId: String,
    val tittel: String?,
    val tema: String?,
    val journalposttype: Journalposttype,
    val journalstatus: Journalstatus,
    val dokumenter: List<DokumentInfo>,
    val avsenderMottaker: AvsenderMottaker,
    val kanal: String?,
    val bruker: Bruker?,
    val sak: JournalpostSak?,
    val datoOpprettet: String,
)

/**
 * [Journalposttype.U] = Utgående
 * [Journalposttype.I] = Inngående
 * [Journalposttype.N] = Notat
 **/
enum class Journalposttype {
    U,
    I,
    N,
}

// https://confluence.adeo.no/display/BOA/Enum%3A+Journalstatus
@Suppress("unused")
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

// https://confluence.adeo.no/display/BOA/Enum%3A+Variantformat
@Suppress("unused")
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
    @JsonProperty("idType")
    @JsonAlias("type")
    val type: AvsenderMottakerIdType?,
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
