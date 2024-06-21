package no.nav.etterlatte.libs.journalpost.dokarkiv

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.etterlatte.libs.journalpost.felles.AvsenderMottaker
import no.nav.etterlatte.libs.journalpost.felles.Bruker
import no.nav.etterlatte.libs.journalpost.felles.JournalpostDokument
import no.nav.etterlatte.libs.journalpost.felles.JournalpostSak
import no.nav.etterlatte.libs.journalpost.felles.JournalpostType

interface OpprettJournalpost {
    val avsenderMottaker: AvsenderMottaker?
    val bruker: Bruker?
    val dokumenter: List<JournalpostDokument>
    val eksternReferanseId: String
    val journalfoerendeEnhet: String
    val journalposttype: JournalpostType
    val kanal: String?
    val sak: JournalpostSak?
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
data class OpprettJournalpostRequest(
    override val avsenderMottaker: AvsenderMottaker?,
    override val bruker: Bruker?,
    override val dokumenter: List<JournalpostDokument>,
    override val eksternReferanseId: String,
    override val journalfoerendeEnhet: String,
    override val journalposttype: JournalpostType,
    override val kanal: String,
    override val sak: JournalpostSak?,
    override val tema: String,
    override val tilleggsopplysninger: Map<String, String> = emptyMap(),
    override val tittel: String,
) : OpprettJournalpost {
    init {
        check(journalposttype != JournalpostType.NOTAT) {
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
    override val journalfoerendeEnhet: String,
    override val sak: JournalpostSak,
    override val tema: String,
    override val tittel: String,
) : OpprettJournalpost {
    override val journalposttype = JournalpostType.NOTAT

    override val kanal = null
    override val avsenderMottaker = null
    override val tilleggsopplysninger = emptyMap<String, String>()
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
