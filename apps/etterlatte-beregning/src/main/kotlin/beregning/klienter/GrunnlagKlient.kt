package no.nav.etterlatte.beregning.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory

interface GrunnlagKlient {
    suspend fun hentGrunnlag(sakId: Long, accessToken: String): Grunnlag
}

class GrunnlagKlientImpl(config: Config, httpClient: HttpClient) : GrunnlagKlient {
    private val logger = LoggerFactory.getLogger(GrunnlagKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("grunnlag.client.id")
    private val resourceUrl = config.getString("grunnlag.resource.url")

    override suspend fun hentGrunnlag(sakId: Long, accessToken: String): Grunnlag {
        logger.info("Henter grunnlag med for sak med id = $sakId")

        return retry<Grunnlag> {
            val json = downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/grunnlag/$sakId"
                    ),
                    accessToken = accessToken
                )
                .mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response

            objectMapper.readValue(json.toString())
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> {
                    logger.error("Klarte ikke hente ut grunnlag for sak med id $sakId. ", it.lastError())
                    throw it.exceptions.last()
                }
            }
        }
    }
}