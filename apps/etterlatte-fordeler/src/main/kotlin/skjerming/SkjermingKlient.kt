package no.nav.etterlatte.skjerming

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getXCorrelationId

class SkjermingKlient(
    private val httpClient: HttpClient,
    private val url: String
) {

    suspend fun personErSkjermet(fnr: String): Boolean {
        return httpClient.post("$url/skjermet") {
            accept(ContentType.Application.Json)
            header(X_CORRELATION_ID, getXCorrelationId())
            header("Nav_Call_Id", getXCorrelationId())
            contentType(ContentType.Application.Json)
            setBody(SkjermetDataRequestDTO(personident = fnr))
        }.body()
    }
}

internal class SkjermetDataRequestDTO(
    val personident: String? = null
)