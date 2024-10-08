package no.nav.etterlatte.vedtaksvurdering.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

interface VilkaarsvurderingKlient {
    suspend fun hentVilkaarsvurdering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VilkaarsvurderingDto?
}

class VilkaarsvurderingKlientException(
    override val message: String,
    override val cause: Throwable,
) : Exception(message, cause)

class BehandlingVilkaarsvurderingKlientImpl(
    config: Config,
    httpClient: HttpClient,
) : VilkaarsvurderingKlient {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    override suspend fun hentVilkaarsvurdering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VilkaarsvurderingDto? {
        logger.info("Henter vilkaarsvurdering med behandlingid=$behandlingId")
        try {
            return retryOgPakkUt {
                downstreamResourceClient
                    .get(
                        resource =
                            Resource(
                                clientId = clientId,
                                url = "$resourceUrl/api/vilkaarsvurdering/$behandlingId",
                            ),
                        brukerTokenInfo = brukerTokenInfo,
                    ).mapBoth(
                        success = { json -> json.response?.let { objectMapper.readValue(it.toString()) } },
                        failure = { throwableErrorMessage -> throw throwableErrorMessage },
                    )
            }
        } catch (e: Exception) {
            throw VilkaarsvurderingKlientException(
                "Henting av vilkårsvurdering for behandling med behandlingId=$behandlingId fra vedtak feilet",
                e,
            )
        }
    }
}
