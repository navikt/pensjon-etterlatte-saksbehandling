package no.nav.etterlatte.brev.pdf

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import no.nav.etterlatte.brev.model.BrevRequest
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class PdfGeneratorKlient(private val client: HttpClient, private val apiUrl: String) {
    private val logger = LoggerFactory.getLogger(PdfGeneratorKlient::class.java)

    @OptIn(ExperimentalTime::class)
    suspend fun genererPdf(brevRequest: BrevRequest): ByteArray = try {
        logger.info("Starter pdfgen")

        measureTimedValue {
            client.post("$apiUrl/${brevRequest.brevMalUrl()}") {
                header("Content-Type", "application/json")
                header(X_CORRELATION_ID, getXCorrelationId())
                setBody(brevRequest.toJsonNode())
            }.body<ByteArray>()
        }.let { (result, duration) ->
            logger.info("Fullført pdfgen OK (${duration.toString(DurationUnit.SECONDS, 2)})")

            result
        }
    } catch (ex: Exception) {
        throw PdfGeneratorException("Feil ved kall til pdfgen", ex)
    }
}

class PdfGeneratorException(msg: String, cause: Throwable) : Exception(msg, cause)

private fun BrevRequest.toJsonNode(): JsonNode = objectMapper.readTree(toJson())
private fun BrevRequest.brevMalUrl(): String = "/${this.templateName()}-${this.spraak.verdi}"