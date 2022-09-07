package no.nav.etterlatte.behandling

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.get
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SaksbehandlerMedlemskapsperioder
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory

interface EtterlatteGrunnlag {
    suspend fun finnPersonOpplysning(
        sakId: Long,
        opplysningsType: Opplysningstyper,
        accessToken: String
    ): Grunnlagsopplysning<Person>?

    suspend fun finnPerioder(
        sakId: Long,
        opplysningsType: Opplysningstyper,
        accessToken: String
    ): Grunnlagsopplysning<SaksbehandlerMedlemskapsperioder>?
}

class GrunnlagKlient(config: Config, private val httpClient: HttpClient) : EtterlatteGrunnlag {
    private val logger = LoggerFactory.getLogger(GrunnlagKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("grunnlag.client.id")
    private val resourceUrl = config.getString("grunnlag.resource.url")

    override suspend fun finnPersonOpplysning(
        sakId: Long,
        opplysningsType: Opplysningstyper,
        accessToken: String
    ): Grunnlagsopplysning<Person>? {
        try {
            logger.info("Henter opplysning ($opplysningsType) fra grunnlag for sak med id $sakId.")

            val json = downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/grunnlag/$sakId/$opplysningsType"
                    ),
                    accessToken = accessToken
                )
                .mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response

            return objectMapper.readValue(json.toString())
        } catch (e: Exception) {
            logger.error("Henting av opplysning ($opplysningsType) fra grunnlag for sak med id $sakId feilet.")
            throw e
        }
    }

    override suspend fun finnPerioder(
        sakId: Long,
        opplysningsType: Opplysningstyper,
        accessToken: String
    ): Grunnlagsopplysning<SaksbehandlerMedlemskapsperioder>? {
        try {
            logger.info("Henter opplysning ($opplysningsType) fra grunnlag for sak med id $sakId.")

            val token = azureAdClient.getOnBehalfOfAccessTokenForResource(
                listOf("api://ce96a301-13db-4409-b277-5b27f464d08b/.default"),
                accessToken
            ).get()?.accessToken ?: ""

            val response = httpClient.get("$resourceUrl/grunnlag/$sakId/$opplysningsType") {
                header("Authorization", "Bearer $token")
                accept(ContentType.Application.Json)
            }

            return when (response.status) {
                HttpStatusCode.OK -> objectMapper.readValue<Grunnlagsopplysning<SaksbehandlerMedlemskapsperioder>>(
                    response.bodyAsText()
                )
                HttpStatusCode.NotFound -> null
                else -> throw Exception("Feil i kall mot grunnlag. Status: ${response.status}")
            }
        } catch (e: Exception) {
            logger.error("Henting av opplysning ($opplysningsType) fra grunnlag for sak med id $sakId feilet.")
            throw e
        }
    }
}