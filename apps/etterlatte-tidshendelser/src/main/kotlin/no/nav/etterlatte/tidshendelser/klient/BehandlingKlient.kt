package no.nav.etterlatte.tidshendelser.klient

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.sak.Sak

class BehandlingKlient(
    private val behandlingHttpClient: HttpClient,
    private val behandlingUrl: String,
) {
    fun hentSaker(sakIder: List<Long>): Map<Long, Sak> {
        if (sakIder.isEmpty()) {
            return emptyMap()
        }

        return runBlocking {
            behandlingHttpClient.post("$behandlingUrl/saker/hent") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(SakIderDto(sakIder))
            }.body<SakerDto>()
        }.saker
    }
}

data class SakIderDto(
    val sakIder: List<Long>,
)

data class SakerDto(
    val saker: Map<Long, Sak>,
)
