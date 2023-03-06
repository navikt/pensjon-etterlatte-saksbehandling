package no.nav.etterlatte.vilkaarsvurdering.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.RetryResult.Failure
import no.nav.etterlatte.libs.common.RetryResult.Success
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.Bruker
import org.slf4j.LoggerFactory

interface GrunnlagKlient {
    suspend fun hentGrunnlag(sakId: Long, bruker: Bruker): Grunnlag
}

class GrunnlagKlientException(override val message: String, override val cause: Throwable) : Exception(message, cause)

class GrunnlagKlientImpl(config: Config, httpClient: HttpClient) : GrunnlagKlient {
    private val logger = LoggerFactory.getLogger(GrunnlagKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("grunnlag.client.id")
    private val resourceUrl = config.getString("grunnlag.resource.url")

    override suspend fun hentGrunnlag(sakId: Long, bruker: Bruker): Grunnlag {
        logger.info("Henter grunnlag for sak med sakId=$sakId")

        return retry<Grunnlag> {
            downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/grunnlag/$sakId"
                    ),
                    bruker = bruker
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage }
                )
        }.let {
            when (it) {
                is Success -> it.content
                is Failure -> {
                    throw GrunnlagKlientException(
                        "Klarte ikke hente grunnlag for sak med sakId=$sakId",
                        it.exceptions.last()
                    )
                }
            }
        }
    }
}