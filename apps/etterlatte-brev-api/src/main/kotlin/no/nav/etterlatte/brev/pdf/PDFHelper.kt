package no.nav.etterlatte.brev.pdf

import no.nav.etterlatte.brev.model.Pdf
import org.apache.pdfbox.Loader
import org.apache.pdfbox.multipdf.PDFMergerUtility
import java.io.ByteArrayOutputStream

class PDFHelper {
    companion object {
        // Kombinerer en liste med PDF-er til én enkelt PDF, og bevarer rekkefølgen fra listen.
        fun kombinerPdfListeTilEnPdf(pdfListe: List<Pdf>): Pdf {
            val pdfMerger = PDFMergerUtility()
            val finalPdf = Loader.loadPDF(pdfListe.first().bytes)

            pdfListe.drop(1).forEach { pdf ->
                val sourcePdf = Loader.loadPDF(pdf.bytes)
                pdfMerger.appendDocument(finalPdf, sourcePdf)
                sourcePdf.close()
            }

            val out = ByteArrayOutputStream()
            finalPdf.save(out)
            finalPdf.close()

            return Pdf(out.toByteArray())
        }
    }
}
