package no.nav.etterlatte.skjerming

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.contentType

class SkjermingKlient(
    private val httpClient: HttpClient,
    private val url: String
) {

    suspend fun personErSkjermet(fnr: String): Boolean {
        return httpClient.get("$url/skjermet?personident=$fnr") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }.body()
    }
}