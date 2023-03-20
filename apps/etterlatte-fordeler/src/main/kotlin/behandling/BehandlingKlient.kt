package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import no.nav.etterlatte.libs.common.behandling.SakType

class BehandlingKlient(
    private val httpClient: HttpClient,
    private val url: String
) {
    suspend fun hentSak(fnr: String, sakType: SakType): Long {
        return httpClient.get("$url/personer/$fnr/saker/$sakType").body<ObjectNode>()["id"].longValue()
    }
}