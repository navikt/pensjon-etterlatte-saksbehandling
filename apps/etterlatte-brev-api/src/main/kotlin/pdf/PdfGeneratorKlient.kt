package pdf

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.*
import io.ktor.client.request.*
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.model.brev.BrevRequest
import org.slf4j.MDC
import java.util.*

class PdfGeneratorKlient(private val client: HttpClient, private val apiUrl: String) {
    suspend fun genererPdf(brevRequest: BrevRequest): ByteArray = client.post("$apiUrl/${brevRequest.templateName()}") {
        header("Content-Type", "application/json")
        header("X-Correlation-ID", MDC.get("X-Correlation-ID") ?: UUID.randomUUID().toString())
        body = brevRequest.toJsonNode()
    }
}

private fun BrevRequest.toJsonNode(): JsonNode = objectMapper.readTree(toJson())
