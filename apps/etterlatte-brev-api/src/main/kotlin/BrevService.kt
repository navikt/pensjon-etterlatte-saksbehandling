package no.nav.etterlatte

import model.Vedtak
import model.VedtakType
import model.brev.InnvilgetBrevRequest
import no.nav.etterlatte.db.BrevRepository
import no.nav.etterlatte.vedtak.VedtakService
import org.slf4j.LoggerFactory
import pdf.PdfGeneratorKlient

class BrevService(
    private val db: BrevRepository,
    private val pdfGenerator: PdfGeneratorKlient
) {
    private val logger = LoggerFactory.getLogger(BrevService::class.java)
    private val vedtakService = VedtakService()

    suspend fun hentBrev(behandlingId: String): ByteArray {
        val vedtak = vedtakService.hentVedtak(behandlingId)

        val brev = db.hentBrev(vedtak.vedtakId.toLong())

        return brev?.data ?: opprett(vedtak)
    }

    private suspend fun opprett(vedtak: Vedtak): ByteArray {
        val pdf = when (vedtak.type) {
            VedtakType.INNVILGELSE -> pdfGenerator.genererPdf(InnvilgetBrevRequest.fraVedtak(vedtak))
            else -> throw Exception("Vedtakstype er ikke støttet: ${vedtak.type}")
        }

        logger.info("Generert brev for vedtak (vedtakId=${vedtak.vedtakId}) med størrelse: ${pdf.size}")

        return db.opprettBrev(vedtak.vedtakId.toLong(), pdf).data
    }
}
