package no.nav.etterlatte.gyldigsoeknad.pdf

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import org.slf4j.MDC
import java.util.UUID

interface PdfGenerator {
    suspend fun genererPdf(
        input: JsonNode,
        template: String,
    ): ByteArray
}

class PdfGeneratorKlient(
    private val client: HttpClient,
    private val apiUrl: String,
) : PdfGenerator {
    override suspend fun genererPdf(
        input: JsonNode,
        template: String,
    ): ByteArray =
        client
            .post("$apiUrl/$template") {
                header("X-Correlation-ID", MDC.get("X-Correlation-ID") ?: UUID.randomUUID().toString())
                setBody(TextContent(input.toPrettyString(), ContentType.Application.Json))
            }.body()
}
