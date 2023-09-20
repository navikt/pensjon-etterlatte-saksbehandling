package no.nav.etterlatte.brev.vilkaarsvurdering

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
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class VilkaarsvurderingKlientException(override val message: String, override val cause: Throwable) :
    Exception(message, cause)

class VilkaarsvurderingKlient(config: Config, httpClient: HttpClient) {
    private val logger = LoggerFactory.getLogger(VilkaarsvurderingKlient::class.java)
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vilkaarsvurdering.client.id")
    private val resourceUrl = config.getString("vilkaarsvurdering.resource.url")

    suspend fun hentVilkaarsvurdering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VilkaarsvurderingDto {
        logger.info("Henter vilkaarsvurdering med behandlingid $behandlingId")
        return retry<VilkaarsvurderingDto> {
            downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/api/vilkaarsvurdering/$behandlingId",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> {
                    throw VilkaarsvurderingKlientException(
                        "Klarte ikke hente vilkåårsvurdering for behandling med behandlingId=$behandlingId",
                        it.samlaExceptions(),
                    )
                }
            }
        }
    }
}
