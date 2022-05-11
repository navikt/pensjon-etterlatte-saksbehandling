package no.nav.etterlatte

import journalpost.JournalpostService
import model.Vedtak
import model.VedtakType
import model.brev.InnvilgetBrevRequest
import no.nav.etterlatte.db.Brev
import no.nav.etterlatte.db.BrevRepository
import model.brev.AvslagBrevRequest
import no.nav.etterlatte.db.Mottaker
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

    suspend fun hentBrev(behandlingId: String): List<Brev> {
        val brev = db.hentBrevForBehandling(behandlingId.toLong())

        return brev.ifEmpty {
//            listOf(opprett(behandlingId))
            emptyList()
        }
    }

    suspend fun opprett(behandlingId: String, mottaker: Mottaker): Brev {

        return db.opprettBrev(behandlingId.toLong(), mottaker)
    }

    suspend fun opprett(behandlingId: String): ByteArray {
        val vedtak = vedtakService.hentVedtak(behandlingId)

        val brev = db.hentBrev(vedtak.vedtakId.toLong())

        return brev?.data ?: genererPdf(vedtak)
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
