package no.nav.etterlatte.brev.brevbaker

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.pensjon.brevbaker.api.model.LetterMarkup
import no.nav.pensjon.brevbaker.api.model.LetterMetadata
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

class BrevbakerKlient(
    val client: HttpClient,
    val apiUrl: String,
) {
    val logger = LoggerFactory.getLogger(BrevbakerKlient::class.java)
    val sikkerlogg = sikkerlogger()

    suspend fun genererPdf(brevRequest: BrevbakerRequest): BrevbakerPdfResponse =
        try {
            measureTimedValue {
                client
                    .post("$apiUrl/etterlatte/pdf") {
                        contentType(ContentType.Application.Json)
                        setBody(brevRequest)
                        timeout {
                            socketTimeoutMillis = Duration.ofMinutes(3).toMillis()
                            requestTimeoutMillis = Duration.ofMinutes(4).toMillis()
                            connectTimeoutMillis = Duration.ofMinutes(1).toMillis()
                        }
                    }.body<BrevbakerPdfResponse>()
            }.let { (result, duration) ->
                logger.info("Fullført brevbaker pdf OK (${duration.toString(DurationUnit.SECONDS, 2)})")
                result
            }
        } catch (ex: Exception) {
            sikkerlogg.error("Brevbaker pdfgen feilet. Request body: ${brevRequest.toJson()}", ex)
            throw BrevbakerException("Feil ved kall til brevbaker (se sikkerlogg)", ex)
        }

    suspend fun genererHTML(brevRequest: BrevbakerRequest): BrevbakerHTMLResponse =
        try {
            measureTimedValue {
                client
                    .post("$apiUrl/etterlatte/html") {
                        contentType(ContentType.Application.Json)
                        setBody(brevRequest.toJsonNode())
                    }.body<BrevbakerHTMLResponse>()
            }.let { (result, duration) ->
                logger.info("Fullført brevbaker HTML OK (${duration.toString(DurationUnit.SECONDS, 2)})")
                result
            }
        } catch (ex: Exception) {
            sikkerlogg.error("Feila ved generer html-kall mot brevbakeren. Requesten var $brevRequest", ex)
            throw BrevbakerException("Feil ved kall til brevbaker", ex)
        }

    suspend fun genererJSON(brevRequest: BrevbakerRequest): LetterMarkup =
        try {
            measureTimedValue {
                client
                    .post("$apiUrl/etterlatte/json") {
                        contentType(ContentType.Application.Json)
                        setBody(brevRequest.toJsonNode())
                    }.body<LetterMarkup>()
            }.let { (result, duration) ->
                logger.info("Fullført brevbaker JSON OK (${duration.toString(DurationUnit.SECONDS, 2)})")
                result
            }
        } catch (ex: Exception) {
            sikkerlogg.error("Feila ved generer json-kall mot brevbakeren. Requesten var $brevRequest", ex)
            throw BrevbakerException("Feil ved kall til brevbaker", ex)
        }
}

class BrevbakerException(
    msg: String,
    cause: Throwable,
) : Exception(msg, cause)

class BrevbakerPdfResponse(
    val base64pdf: String,
    val letterMetadata: LetterMetadata,
)

class BrevbakerHTMLResponse(
    val html: Map<String, String>,
    val letterMetadata: LetterMetadata,
)

private fun BrevbakerRequest.toJsonNode(): JsonNode = objectMapper.readTree(toJson())
