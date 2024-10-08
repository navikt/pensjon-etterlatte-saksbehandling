package no.nav.etterlatte.brev.hentinformasjon.vilkaarsvurdering

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.vilkaarsvurdering.MigrertYrkesskadefordel
import org.slf4j.LoggerFactory
import java.util.UUID

class VilkaarsvurderingKlientException(
    override val message: String,
    override val cause: Throwable,
) : Exception(message, cause)

class BehandlingVilkaarsvurderingKlient(
    config: Config,
    httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(BehandlingVilkaarsvurderingKlient::class.java)
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    internal suspend fun hentVilkaarsvurdering(
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
                ).mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> {
                    throw VilkaarsvurderingKlientException(
                        "Klarte ikke hente vilkårsvurdering for behandling med behandlingId=$behandlingId",
                        it.samlaExceptions(),
                    )
                }
            }
        }
    }

    internal suspend fun erMigrertYrkesskade(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): Boolean =
        downstreamResourceClient
            .get(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/vilkaarsvurdering/$behandlingId/migrert-yrkesskadefordel",
                    ),
                brukerTokenInfo = bruker,
            ).mapBoth(
                success = { resource ->
                    resource.response.let { objectMapper.readValue<MigrertYrkesskadefordel>(it.toString()) }.migrertYrkesskadefordel
                },
                failure = { throwableErrorMessage -> throw throwableErrorMessage },
            )
}
