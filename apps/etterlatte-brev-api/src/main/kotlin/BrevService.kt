package no.nav.etterlatte

import model.VedtakType
import model.brev.InnvilgetBrevRequest
import no.nav.etterlatte.vedtak.VedtakService
import org.slf4j.LoggerFactory
import pdf.PdfGeneratorKlient

class BrevService(private val pdfGenerator: PdfGeneratorKlient) {
    private val logger = LoggerFactory.getLogger(BrevService::class.java)
    private val vedtakService = VedtakService()

    suspend fun opprettBrev(vedtakId: String): ByteArray {
        val vedtak = vedtakService.hentVedtak(vedtakId)

        return when (vedtak.type) {
            VedtakType.INNVILGELSE -> pdfGenerator.genererPdf(InnvilgetBrevRequest.fraVedtak(vedtak)).also {
                logger.info("Generert brev for vedtak (vedtakId=${vedtak.vedtakId}) med størrelse: ${it.size}")
            }
            else -> throw Exception("Vedtakstype er ikke støttet: ${vedtak.type}")
        }
    }
}
