package no.nav.etterlatte.journalpost

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.etterlatte.libs.common.journalpost.JournalpostRequest
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import no.nav.etterlatte.libs.common.toJson
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.*

class JournalpostKlient(private val client: HttpClient, private val url: String) {
    private val logger = LoggerFactory.getLogger(JournalpostKlient::class.java)

    suspend fun opprettJournalpost(request: JournalpostRequest, ferdigstill: Boolean): JournalpostResponse = try {
        client.post("$url?forsoekFerdigstill=$ferdigstill") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header(X_CORRELATION_ID, getXCorrelationId())
            body = request.toJson()
        }
    } catch (responseException: ResponseException) {
        logger.error("Feil i kall mot Dokarkiv: ", responseException)

        throw when (responseException.response.status.value) {
            HttpStatusCode.Conflict.value -> DuplikatJournalpostException("Duplikat journalpost", responseException)
            else -> JournalpostException("Feil i kall mot Dokarkiv", responseException)
        }
    } catch (exception: Exception) {
        logger.error("Feil i kall mot Dokarkiv: ", exception)
        throw JournalpostException("Feil i kall mot Dokarkiv", exception)
    }
}

open class JournalpostException(msg: String, cause: Throwable) : Exception(msg, cause)
class DuplikatJournalpostException(msg: String, cause: Throwable) : JournalpostException(msg, cause)
