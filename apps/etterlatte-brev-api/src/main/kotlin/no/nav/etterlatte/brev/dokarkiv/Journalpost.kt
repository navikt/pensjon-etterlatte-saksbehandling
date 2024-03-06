package no.nav.etterlatte.brev.dokarkiv

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

interface OpprettJournalpost

/**
 * Requestobjekt for å opprette ny Journalpost
 *
 * Tema:
 *  https://confluence.adeo.no/display/BOA/Tema
 * Kanal:
 *  https://confluence.adeo.no/display/BOA/Utsendingskanal
 **/
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class OpprettJournalpostRequest(
    val avsenderMottaker: AvsenderMottaker,
    val behandlingstema: String? = null,
    val bruker: Bruker,
    val datoDokument: LocalDateTime? = null,
    val datoMottatt: LocalDateTime? = null,
    val dokumenter: List<JournalpostDokument>,
    val eksternReferanseId: String,
    val journalfoerendeEnhet: String,
    val journalposttype: JournalPostType,
    val kanal: String,
    val sak: JournalpostSak,
    val tema: String,
    val tilleggsopplysninger: Map<String, String> = emptyMap(),
    val tittel: String,
) : OpprettJournalpost

/**
 * Requestobjekt for å opprette journalpost av typen NOTAT
 *
 * Det er en del felter som *ikke* skal settes, kontra en journalpost med type INNGAAENDE / UTGAAENDE:
 *  - avsenderMottaker skal ikke settes
 *  - datoMottatt skal ikke settes
 *  - kanal skal ikke settes
 *  - journalposttypen skal være NOTAT
 *
 *  Derfor en egen type for opprettingen av notater.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class OpprettNotatJournalpostRequest(
    val behandlingstema: String? = null,
    val bruker: Bruker,
    val dokumenter: List<JournalpostDokument>,
    val datoDokument: LocalDateTime? = null,
    val eksternReferanseId: String,
    val journalfoerendeEnhet: String,
    val sak: JournalpostSak,
    val tema: String,
    val tilleggsopplysninger: Map<String, String> = emptyMap(),
    val tittel: String,
) : OpprettJournalpost {
    val journalposttype: String = "NOTAT"
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpprettJournalpostResponse(
    val journalpostId: String,
    val journalpostferdigstilt: Boolean,
    val dokumenter: List<DokumentInfo> = emptyList(),
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DokumentInfo(
        val dokumentInfoId: String,
    )
}

data class OppdaterJournalpostResponse(
    val journalpostId: String,
)

data class KnyttTilAnnenSakRequest(
    val bruker: Bruker,
    val fagsakId: String,
    val fagsaksystem: String,
    val journalfoerendeEnhet: String,
    val tema: String,
    val sakstype: Sakstype,
)

data class KnyttTilAnnenSakResponse(
    val nyJournalpostId: String,
)

data class AvsenderMottaker(
    val id: String?,
    val idType: String? = "FNR",
    val navn: String? = null,
    val land: String? = null,
)

data class Bruker(
    val id: String,
    val idType: BrukerIdType = BrukerIdType.FNR,
)

data class JournalpostDokument(
    val tittel: String,
    val brevkode: String = "XX.YY-ZZ",
    val dokumentvarianter: List<DokumentVariant>,
)

data class JournalpostSak(
    val sakstype: Sakstype,
    val fagsakId: String? = null,
    val tema: String? = null,
    val fagsaksystem: String? = null,
) {
    init {
        if (sakstype == Sakstype.FAGSAK) {
            check(!fagsakId.isNullOrBlank()) { "fagsakId må være satt når sakstype=${Sakstype.FAGSAK}" }
            check(!fagsaksystem.isNullOrBlank()) { "fagsaksystem må være satt når sakstype=${Sakstype.FAGSAK}" }
        }
    }
}

enum class Sakstype {
    FAGSAK,
    GENERELL_SAK,
}

sealed class DokumentVariant {
    abstract val filtype: String
    abstract val fysiskDokument: String
    abstract val variantformat: String

    data class ArkivPDF(
        override val fysiskDokument: String,
    ) : DokumentVariant() {
        override val filtype: String = "PDFA"
        override val variantformat: String = "ARKIV"
    }

    data class OriginalJson(
        override val fysiskDokument: String,
    ) : DokumentVariant() {
        override val filtype: String = "JSON"
        override val variantformat: String = "ORIGINAL"
    }
}

enum class JournalPostType(val type: String) {
    INNGAAENDE("INNGAAENDE"),
    UTGAAENDE("UTGAAENDE"),
}

enum class DokumentKategori(val type: String) {
    SOK("SOK"),
    VB("VB"),
    IB("IB"),
}

enum class BrukerIdType {
    FNR,
    AKTOERID,
    ORGNR,
}

class JournalpostKoder {
    companion object {
        const val BREV_KODE = "XX.YY-ZZ"
    }
}
