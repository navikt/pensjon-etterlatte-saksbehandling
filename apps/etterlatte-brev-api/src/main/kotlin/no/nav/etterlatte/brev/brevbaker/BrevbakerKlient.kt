package no.nav.etterlatte.brev.brevbaker

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import no.nav.etterlatte.brev.model.BrevRequest
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class BrevbakerKlient(private val client: HttpClient, private val apiUrl: String) {
    private val logger = LoggerFactory.getLogger(BrevbakerKlient::class.java)

    @OptIn(ExperimentalTime::class)
    suspend fun genererPdf(brevRequest: BrevbakerRequest): ByteArray = try {
        measureTimedValue {
            client.post("$apiUrl/hvaennderevilha") {
                header("Content-Type", "application/json")
                setBody(brevRequest)
            }.body<ByteArray>()
        }.let { (result, duration) ->
            logger.info("Fullført pdfgen OK (${duration.toString(DurationUnit.SECONDS, 2)})")

            result
        }
    } catch (ex: Exception) {
        throw BrevbakerException("Feil ved kall til brevbaker", ex)
    }
}

class BrevbakerException(msg: String, cause: Throwable) : Exception(msg, cause)

private fun BrevRequest.toJsonNode(): JsonNode = objectMapper.readTree(toJson())
private fun BrevRequest.brevMalUrl(): String = "/${this.templateName()}-${this.spraak.verdi}"

data class EtterlatteBrevDto(val navn: String)