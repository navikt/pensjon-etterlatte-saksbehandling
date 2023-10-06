package dolly

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.testdata.dolly.DollyPersonResponse

class TestnavClient(private val httpClient: HttpClient, private val url: String) {
    suspend fun hentPersonInfo(
        identer: List<String>,
        accessToken: String,
    ): List<DollyPersonResponse> =
        httpClient.get("$url/v2/personer/identer?identer=${identer.joinToString(",")}") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }.let {
            objectMapper.readValue(it.body<JsonNode>()["data"]["hentPersonBolk"].toJson())
        }
}
