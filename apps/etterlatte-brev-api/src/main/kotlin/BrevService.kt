package no.nav.etterlatte

import journalpost.JournalpostService
import model.Vedtak
import model.VedtakType
import model.brev.InnvilgetBrevRequest
import no.nav.etterlatte.db.Brev
import no.nav.etterlatte.db.BrevRepository
import model.brev.AvslagBrevRequest
import no.nav.etterlatte.db.Adresse
import no.nav.etterlatte.db.BrevID
import no.nav.etterlatte.db.Mottaker
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.vedtak.VedtakService
import org.slf4j.LoggerFactory
import pdf.PdfGeneratorKlient

class BrevService(
    private val db: BrevRepository,
    private val pdfGenerator: PdfGeneratorKlient,
    private val journalpostService: JournalpostService
) {
    private val logger = LoggerFactory.getLogger(BrevService::class.java)
    private val vedtakService = VedtakService()

    suspend fun hentAlleBrev(behandlingId: String): List<Brev> {
        val brev = db.hentBrevForBehandling(behandlingId.toLong())

        return brev.ifEmpty {
            logger.info("Ingen brev funnet for behandling $behandlingId. Forsøker å opprette fra vedtak.")
            listOf(opprettFraVedtak(behandlingId))
        }
    }

    fun hentBrevMedId(id: BrevID): Brev = db.hentBrev(id)

    fun hentBrevInnhold(id: BrevID): ByteArray = db.hentBrevInnhold(id)

    fun opprett(behandlingId: String, mottaker: Mottaker, mal: String): Brev {
        // TODO: Fikse støtte for mal
        return db.opprettBrev(behandlingId.toLong(), mottaker)
    }

    fun ferdigstillBrev(id: BrevID): Brev {
        db.oppdaterStatus(id, "FERDIGSTILT")

        return hentBrevMedId(id)
    }

    suspend fun opprettFraVedtak(behandlingId: String): Brev {
        // TODO: Håndtere tilfeller hvor vedtak mangler ...
        val vedtak = vedtakService.hentVedtak(behandlingId)

        val navn = vedtak.barn.navn.split(" ")
        val mottaker = Mottaker(
            fornavn = navn[0],
            etternavn= navn[1],
            foedselsnummer = Foedselsnummer.of(vedtak.barn.fnr),
            adresse = Adresse("Fyrstikkalléen 1", "0661", "Oslo")
        )

        val pdf = genererPdf(vedtak)

        return db.opprettBrev(behandlingId.toLong(), mottaker, pdf)
    }

    private suspend fun genererPdf(vedtak: Vedtak): ByteArray {
        val pdfRequest = when (vedtak.type) {
            VedtakType.INNVILGELSE -> InnvilgetBrevRequest.fraVedtak(vedtak)
            VedtakType.AVSLAG -> AvslagBrevRequest.fraVedtak(vedtak)
            else -> throw Exception("Vedtakstype er ikke støttet: ${vedtak.type}")
        }

        val pdf = pdfGenerator.genererPdf(pdfRequest)

        logger.info("Generert brev for vedtak (vedtakId=${vedtak.vedtakId}) med størrelse: ${pdf.size}")

        return pdf
    }

    // Må nok rydde opp i hvordan brev lenkes opp i databasen.
    suspend fun sendBrev(behandlingId: String) {
        val vedtak = vedtakService.hentVedtak(behandlingId)
        val brev = db.hentBrev(vedtak.vedtakId.toLong())
            ?: throw Exception("Klarte ikke finne brev tilhørende vedtak med vedtakId ${vedtak.vedtakId}")

        journalpostService.journalfoer(vedtak, brev)
    }
}
