package no.nav.etterlatte.testdata.dolly

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface DollyClient {
    suspend fun hentDollyBrukere(accessToken: String): List<Bruker>
    suspend fun hentBrukersGrupper(brukerId: String, accessToken: String): HentGruppeResponse
    suspend fun opprettTestGruppe(gruppe: OpprettGruppeRequest, accessToken: String): Gruppe
    suspend fun opprettBestilling(bestilling: String, gruppeId: Long, accessToken: String): BestillingStatus
    suspend fun hentTestGruppeBestillinger(
        gruppeId: Long,
        accessToken: String,
        pageNo: Int,
        pageSize: Int
    ): TestGruppeBestillinger

    suspend fun hentPersonInfo(identer: List<String>, accessToken: String): List<DollyPersonResponse>
    suspend fun markerIdentIBruk(ident: String, accessToken: String): DollyIBrukResponse
}

class DollyClientImpl(config: Config, private val httpClient: HttpClient) : DollyClient {
    private val logger: Logger = LoggerFactory.getLogger(DollyClientImpl::class.java)

    private val dollyUrl = config.getString("dolly.resource.url")

    override suspend fun hentDollyBrukere(accessToken: String): List<Bruker> {
        logger.info("henter dollyting på url: $dollyUrl")
        return httpClient.get("$dollyUrl/bruker") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }.body()
    }

    override suspend fun hentBrukersGrupper(brukerId: String, accessToken: String): HentGruppeResponse =
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
            header(HttpHeaders.ContentType, ContentType.Application.Json)
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
            objectMapper.readValue(it.body<JsonNode>()["data"]["hentPersonBolk"].toJson())
        }

    override suspend fun markerIdentIBruk(ident: String, accessToken: String): DollyIBrukResponse =
        httpClient.put("$dollyUrl/ident/$ident/ibruk?iBruk=true") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }.body()
}