package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.libs.common.retryOgPakkUt
import org.slf4j.LoggerFactory
import java.util.Base64

class BrevbakerService(
    private val brevbakerKlient: BrevbakerKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun genererPdf(
        brevID: BrevID?,
        brevRequest: BrevbakerRequest,
    ): Pdf {
        logger.info("Genererer PDF med Brevbakeren med kode ${brevRequest.kode} og id $brevID")

        val brevbakerResponse = retryOgPakkUt { brevbakerKlient.genererPdf(brevRequest) }

        return Base64
            .getDecoder()
            .decode(brevbakerResponse.file)
            .let { Pdf(it) }
            .also { logger.info("Generert brev (id=$brevID) med størrelse: ${it.bytes.size}") }
    }

    suspend fun hentRedigerbarTekstFraBrevbakeren(request: BrevbakerRequest): Slate {
        val brevbakerResponse = retryOgPakkUt { brevbakerKlient.genererJSON(request) }
        return BlockTilSlateKonverterer.konverter(brevbakerResponse)
    }
}
