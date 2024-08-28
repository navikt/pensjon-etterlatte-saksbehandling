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

class PdfGeneratorKlient(
    private val client: HttpClient,
    private val apiUrl: String,
) {
    suspend fun genererPdf(
        payload: JsonNode,
        mal: String,
    ): ByteArray =
        client
            .post("$apiUrl/$mal") {
                header(CORRELATION_ID, getCorrelationId())
                contentType(ContentType.Application.Json)
                setBody(TextContent(payload.toPrettyString(), ContentType.Application.Json))
            }.body()
}
