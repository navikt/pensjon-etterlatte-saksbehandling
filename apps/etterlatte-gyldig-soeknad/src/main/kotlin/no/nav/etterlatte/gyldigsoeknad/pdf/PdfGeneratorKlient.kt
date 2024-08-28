package no.nav.etterlatte.gyldigsoeknad.pdf

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PDFMal
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.toJsonNode
import org.slf4j.LoggerFactory

class PdfGeneratorKlient(
    private val klient: HttpClient,
    private val apiUrl: String,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun genererPdf(
        payload: PDFMal,
        mal: String,
    ) = genererPdf(TextContent(payload.toJsonNode().toPrettyString(), ContentType.Application.Json), mal, mal)

    private suspend fun genererPdf(
        body: Any,
        mal: String,
        sti: String,
    ): ByteArray {
        logger.info("Genererer PDF med ey-pdfgen (mal=$mal)")

        return klient
            .post("$apiUrl/$sti") {
                header(CORRELATION_ID, getCorrelationId())
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
    }
}
