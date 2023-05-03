package no.nav.etterlatte.brev.brevbaker

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.pensjon.brev.api.model.LetterMetadata
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class BrevbakerKlient(private val client: HttpClient, private val apiUrl: String) {
    private val logger = LoggerFactory.getLogger(BrevbakerKlient::class.java)

    @OptIn(ExperimentalTime::class)
    suspend fun genererPdf(brevRequest: BrevbakerRequest): BrevbakerResponse = try {
        measureTimedValue {
            client.post("$apiUrl/etterlatte/hvaennderevilha") {
                contentType(ContentType.Application.Json)
                setBody(brevRequest.toJsonNode())
            }.body<BrevbakerResponse>()
        }.let { (result, duration) ->
            logger.info("Fullført brevbaker pdf OK (${duration.toString(DurationUnit.SECONDS, 2)})")

            result
        }
    } catch (ex: Exception) {
        throw BrevbakerException("Feil ved kall til brevbaker", ex)
    }
}

class BrevbakerException(msg: String, cause: Throwable) : Exception(msg, cause)

class BrevbakerResponse(val base64pdf: String, val letterMetadata: LetterMetadata)

private fun BrevbakerRequest.toJsonNode(): JsonNode = objectMapper.readTree(toJson())

data class EtterlatteBrevDto(val navn: String)