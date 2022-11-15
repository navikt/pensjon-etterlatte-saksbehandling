package no.nav.etterlatte.distribusjon

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.distribusjon.DistribuerJournalpostRequest
import no.nav.etterlatte.libs.common.distribusjon.DistribuerJournalpostResponse
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import no.nav.etterlatte.libs.common.toJson
import org.slf4j.LoggerFactory

class DistribusjonKlient(private val client: HttpClient, private val url: String) {
    private val logger = LoggerFactory.getLogger(DistribusjonKlient::class.java)

    suspend fun distribuerJournalpost(request: DistribuerJournalpostRequest): DistribuerJournalpostResponse = try {
        client.post("$url/distribuerjournalpost") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header(X_CORRELATION_ID, getXCorrelationId())
            setBody(request)
        }.body()
    } catch (exception: Exception) {
        logger.error("Feil i kall mot dokumentdistribusjon: ", exception)
        throw DistribusjonException("Feil i kall mot dokumentdistribusjon", exception)
    }
}

open class DistribusjonException(msg: String, cause: Throwable) : Exception(msg, cause)