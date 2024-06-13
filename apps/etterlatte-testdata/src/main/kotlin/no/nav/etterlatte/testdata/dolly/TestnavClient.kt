package no.nav.etterlatte.testdata.dolly

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import no.nav.etterlatte.getTestnavAccessToken
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson

class TestnavClient(
    private val config: Config,
    private val httpClient: HttpClient,
) {
    private val url = config.getString("testnav.resource.url")

    suspend fun hentPersonInfo(identer: List<String>): List<DollyPersonResponse> =
        httpClient
            .get("$url/personer/identer?identer=${identer.joinToString(",")}") {
                header(HttpHeaders.Authorization, "Bearer ${getTestnavAccessToken()}")
            }.let {
                objectMapper.readValue(it.body<JsonNode>()["data"]["hentPersonBolk"].toJson())
            }
}
