package no.nav.etterlatte.brev.distribusjon

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import org.slf4j.LoggerFactory

class DistribusjonKlient(private val client: HttpClient, private val url: String) {
    private val logger = LoggerFactory.getLogger(DistribusjonKlient::class.java)

    suspend fun distribuerJournalpost(request: DistribuerJournalpostRequest): DistribuerJournalpostResponse = try {
        client.post("$url/distribuerjournalpost") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header(X_CORRELATION_ID, getXCorrelationId())
            setBody(request)
        }.let {
            when (it.status) {
                HttpStatusCode.OK -> it.body()
                HttpStatusCode.Conflict -> it.body()
                else -> throw ResponseException(it, "Ukjent respons fra dokumentdistribusjon")
            }
        }
    } catch (exception: Exception) {
        logger.error("Feil i kall mot dokumentdistribusjon: ", exception)
        throw DistribusjonException("Feil i kall mot dokumentdistribusjon", exception)
    }
}

open class DistribusjonException(msg: String, cause: Throwable) : Exception(msg, cause)