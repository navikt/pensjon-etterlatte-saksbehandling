package pdf

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.model.brev.BrevRequest
import org.slf4j.MDC
import java.util.*

class PdfGeneratorKlient(private val client: HttpClient, private val apiUrl: String) {
    suspend fun genererPdf(brevRequest: BrevRequest): ByteArray = try {
        client.post(apiUrl + brevRequest.brevMalUrl()) {
            header("Content-Type", "application/json")
            header("X-Correlation-ID", MDC.get("X-Correlation-ID") ?: UUID.randomUUID().toString())
            setBody(brevRequest.toJsonNode())
        }.body()
    } catch (ex: Exception) {
        throw PdfGeneratorException("Feil ved kall til pdfgen", ex)
    }
}

class PdfGeneratorException(msg: String, cause: Throwable) : Exception(msg, cause)

private fun BrevRequest.toJsonNode(): JsonNode = objectMapper.readTree(toJson())
private fun BrevRequest.brevMalUrl(): String = "/${this.templateName()}-${this.spraak.verdi}"