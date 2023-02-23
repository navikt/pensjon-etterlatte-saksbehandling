package no.nav.etterlatte.beregning.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.AccessTokenWrapper
import org.slf4j.LoggerFactory
import java.util.*

interface VilkaarsvurderingKlient {
    suspend fun hentVilkaarsvurdering(behandlingId: UUID, accessToken: AccessTokenWrapper): VilkaarsvurderingDto
}

class VilkaarsvurderingKlientException(override val message: String, override val cause: Throwable) :
    Exception(message, cause)

class VilkaarsvurderingKlientImpl(config: Config, httpClient: HttpClient) : VilkaarsvurderingKlient {
    private val logger = LoggerFactory.getLogger(VilkaarsvurderingKlientImpl::class.java)
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vilkaarsvurdering.client.id")
    private val resourceUrl = config.getString("vilkaarsvurdering.resource.url")

    override suspend fun hentVilkaarsvurdering(
        behandlingId: UUID,
        accessToken: AccessTokenWrapper
    ): VilkaarsvurderingDto {
        logger.info("Henter vilkaarsvurdering med behandlingid $behandlingId")
        return retry<VilkaarsvurderingDto> {
            downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/vilkaarsvurdering/$behandlingId"
                    ),
                    accessToken = accessToken
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage }
                )
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> {
                    throw VilkaarsvurderingKlientException(
                        "Klarte ikke hente vilkåårsvurdering for behandling med behandlingId=$behandlingId",
                        it.exceptions.last()
                    )
                }
            }
        }
    }
}