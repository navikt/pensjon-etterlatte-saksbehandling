package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.FoedselsnummerDTO
import no.nav.etterlatte.libs.common.behandling.SakType

class BehandlingKlient(
    private val httpClient: HttpClient,
    private val url: String
) {
    suspend fun hentSak(fnr: String, sakType: SakType): Long {
        return httpClient.post("$url/personer/saker/$sakType") {
            contentType(ContentType.Application.Json)
            setBody(FoedselsnummerDTO(fnr))
        }.body<ObjectNode>()["id"].longValue()
    }
}