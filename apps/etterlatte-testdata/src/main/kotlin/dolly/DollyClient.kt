package dolly

import com.typesafe.config.Config
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.logger

interface DollyClient {
    suspend fun hentDollyBrukere(accessToken: String): List<Bruker>
    suspend fun hentBrukersGrupper(brukerId: String, accessToken: String): List<Gruppe>
    suspend fun opprettTestGruppe(gruppe: OpprettGruppeRequest, accessToken: String): Gruppe
    suspend fun opprettBestilling(bestilling: String, gruppeId: Long, accessToken: String): BestillingStatus
    suspend fun hentTestGruppeBestillinger(gruppeId: Long, accessToken: String, pageNo: Int, pageSize: Int): TestGruppeBestillinger
    suspend fun hentPersonInfo(identer: List<String>, accessToken: String): List<DollyPersonResponse>
}

class DollyClientImpl(config: Config, private val httpClient: HttpClient) : DollyClient {
    private val dollyUrl = config.getString("dolly.resource.url")

    override suspend fun hentDollyBrukere(accessToken: String): List<Bruker> =
        httpClient.get("$dollyUrl/bruker") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }.body()

    override suspend fun hentBrukersGrupper(brukerId: String, accessToken: String): List<Gruppe> =
        httpClient.get("$dollyUrl/gruppe?brukerId=$brukerId") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }.body()

    // Denne vil ikke fungere med client authorization grant.
    override suspend fun opprettTestGruppe(gruppe: OpprettGruppeRequest, accessToken: String): Gruppe =
        httpClient.post("$dollyUrl/gruppe") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(gruppe.toJson())
        }.body()

    override suspend fun opprettBestilling(bestilling: String, gruppeId: Long, accessToken: String): BestillingStatus =
        httpClient.post("$dollyUrl/gruppe/$gruppeId/bestilling") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(objectMapper.readTree(bestilling))
        }.body()

    override suspend fun hentTestGruppeBestillinger(
        gruppeId: Long,
        accessToken: String,
        pageNo: Int,
        pageSize: Int
    ): TestGruppeBestillinger =
        httpClient.get("$dollyUrl/gruppe/$gruppeId/page/$pageNo?pageSize=$pageSize") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }.body()

    override suspend fun hentPersonInfo(identer: List<String>, accessToken: String): List<DollyPersonResponse> =
        httpClient.get("$dollyUrl/pdlperson/identer?identer=${identer.joinToString(",")}") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }.let {
            logger.info(it.bodyAsText())
            it.body()
        }
}
