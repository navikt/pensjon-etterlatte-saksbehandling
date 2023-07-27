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
import no.nav.pensjon.brevbaker.api.model.LetterMetadata
import no.nav.pensjon.brevbaker.api.model.RenderedJsonLetter
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class BrevbakerKlient(private val client: HttpClient, private val apiUrl: String) {
    private val logger = LoggerFactory.getLogger(BrevbakerKlient::class.java)

    @OptIn(ExperimentalTime::class)
    suspend fun genererPdf(brevRequest: BrevbakerRequest): BrevbakerPdfResponse = try {
        measureTimedValue {
            client.post("$apiUrl/etterlatte/pdf") {
                contentType(ContentType.Application.Json)
                setBody(brevRequest.toJsonNode())
            }.body<BrevbakerPdfResponse>()
        }.let { (result, duration) ->
            logger.info("Fullført brevbaker pdf OK (${duration.toString(DurationUnit.SECONDS, 2)})")
            result
        }
    } catch (ex: Exception) {
        throw BrevbakerException("Feil ved kall til brevbaker", ex)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun genererHTML(brevRequest: BrevbakerRequest): BrevbakerHTMLResponse = try {
        measureTimedValue {
            client.post("$apiUrl/etterlatte/html") {
                contentType(ContentType.Application.Json)
                setBody(brevRequest.toJsonNode())
            }.body<BrevbakerHTMLResponse>()
        }.let { (result, duration) ->
            logger.info("Fullført brevbaker HTML OK (${duration.toString(DurationUnit.SECONDS, 2)})")
            result
        }
    } catch (ex: Exception) {
        throw BrevbakerException("Feil ved kall til brevbaker", ex)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun genererJSON(brevRequest: BrevbakerRequest): RenderedJsonLetter = try {
        measureTimedValue {
            client.post("$apiUrl/etterlatte/json") {
                contentType(ContentType.Application.Json)
                setBody(brevRequest.toJsonNode())
            }.body<RenderedJsonLetter>()
        }.let { (result, duration) ->
            logger.info("Fullført brevbaker JSON OK (${duration.toString(DurationUnit.SECONDS, 2)})")
            result
        }
    } catch (ex: Exception) {
        throw BrevbakerException("Feil ved kall til brevbaker", ex)
    }
}

class BrevbakerException(msg: String, cause: Throwable) : Exception(msg, cause)

class BrevbakerPdfResponse(val base64pdf: String, val letterMetadata: LetterMetadata)

class BrevbakerHTMLResponse(val html: Map<String, String>, val letterMetadata: LetterMetadata)

class BrevbakerJSONResponse(val json: RenderedJsonLetter, val letterMetadata: LetterMetadata)

private fun BrevbakerRequest.toJsonNode(): JsonNode = objectMapper.readTree(toJson())