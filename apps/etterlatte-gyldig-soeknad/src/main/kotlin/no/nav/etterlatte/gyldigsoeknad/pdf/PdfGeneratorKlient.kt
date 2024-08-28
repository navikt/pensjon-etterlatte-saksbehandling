package no.nav.etterlatte.gyldigsoeknad.pdf

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import org.slf4j.LoggerFactory

class PdfGeneratorKlient(
    private val klient: HttpClient,
    private val apiUrl: String,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun genererPdf(
        payload: JsonNode,
        mal: String,
    ): ByteArray {
        logger.info("Genererer PDF med ey-pdfgen (mal=$mal)")

        return klient
            .post("$apiUrl/$mal") {
                header(CORRELATION_ID, getCorrelationId())
                contentType(ContentType.Application.Json)
                setBody(TextContent(payload.toPrettyString(), ContentType.Application.Json))
            }.body()
    }
}
