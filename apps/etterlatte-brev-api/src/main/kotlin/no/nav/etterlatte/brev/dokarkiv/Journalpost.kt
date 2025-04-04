package no.nav.etterlatte.brev.dokarkiv

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.feilhaandtering.krev

interface OpprettJournalpost {
    val avsenderMottaker: AvsenderMottaker?
    val bruker: Bruker
    val dokumenter: List<JournalpostDokument>
    val eksternReferanseId: String
    val journalfoerendeEnhet: Enhetsnummer
    val journalposttype: JournalPostType
    val kanal: String?
    val sak: JournalpostSak
    val tema: String
    val tittel: String
    val tilleggsopplysninger: Map<String, String>
}

/**
 * Requestobjekt for å opprette ny Journalpost
 *
 * Tema:
 *  https://confluence.adeo.no/display/BOA/Tema
 * Kanal:
 *  https://confluence.adeo.no/display/BOA/Utsendingskanal
 **/
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class JournalpostRequest(
    override val avsenderMottaker: AvsenderMottaker,
    override val bruker: Bruker,
    override val dokumenter: List<JournalpostDokument>,
    override val eksternReferanseId: String,
    override val journalfoerendeEnhet: Enhetsnummer,
    override val journalposttype: JournalPostType,
    override val kanal: String,
    override val sak: JournalpostSak,
    override val tema: String,
    override val tilleggsopplysninger: Map<String, String> = emptyMap(),
    override val tittel: String,
) : OpprettJournalpost {
    init {
        krev(journalposttype != JournalPostType.NOTAT) {
            "${this::class.simpleName} skal ikke brukes til opprettelse av Notat. " +
                "Bruk ${OpprettNotatJournalpostRequest::class.simpleName} i stedet."
        }
    }
}

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
    override val bruker: Bruker,
    override val dokumenter: List<JournalpostDokument>,
    override val eksternReferanseId: String,
    override val journalfoerendeEnhet: Enhetsnummer,
    override val sak: JournalpostSak,
    override val tema: String,
    override val tittel: String,
) : OpprettJournalpost {
    override val journalposttype = JournalPostType.NOTAT

    override val kanal = null
    override val avsenderMottaker = null
    override val tilleggsopplysninger = emptyMap<String, String>()
}

data class OppdaterJournalpostResponse(
    val journalpostId: String,
)

data class DokarkivErrorResponse(
    val status: Int,
    val error: String?,
    val message: String?,
    val path: String?,
)

data class KnyttTilAnnenSakRequest(
    val bruker: Bruker,
    val fagsakId: String,
    val fagsaksystem: String,
    val journalfoerendeEnhet: Enhetsnummer,
    val tema: String,
    val sakstype: Sakstype,
)

data class KnyttTilAnnenSakResponse(
    val nyJournalpostId: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AvsenderMottaker(
    val id: String?,
    @JsonAlias("type")
    val idType: AvsenderMottakerIdType?,
    val navn: String? = null,
    val land: String? = null,
)

data class Bruker(
    val id: String,
    @JsonAlias("type")
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
)

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
}

enum class JournalPostType(
    val type: String,
) {
    NOTAT("NOTAT"),
    INNGAAENDE("INNGAAENDE"),
    UTGAAENDE("UTGAAENDE"),
}

enum class BrukerIdType {
    FNR,
    AKTOERID,
    ORGNR,
}

/**
 * SAF kan gi oss "NULL" og "UKJENT" som idType, men Dokarkiv støtter ikke at vi sender dette tilbake.
 * Sikrer derfor at idType blir [null] i tilfeller hvor vi ikke anser det som en gyldig verdi.
 **/
enum class AvsenderMottakerIdType {
    FNR,
    ORGNR,
    HPRNR,
    UTL_ORG,
    ;

    companion object {
        @JvmStatic
        @JsonCreator
        fun fra(value: String?) =
            AvsenderMottakerIdType.entries
                .firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

class JournalpostKoder {
    companion object {
        const val BREV_KODE = "XX.YY-ZZ"
    }
}
