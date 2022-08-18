package dolly

import com.typesafe.config.Config
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson

interface DollyClient {
    suspend fun hentDollyBrukere(accessToken: String): List<Bruker>
    suspend fun hentBrukersGrupper(brukerId: String, accessToken: String): List<Gruppe>
    suspend fun opprettTestGruppe(gruppe: OpprettGruppeRequest, accessToken: String): Gruppe
    suspend fun opprettBestilling(bestilling: String, gruppeId: Long, accessToken: String): BestillingStatus
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
}
