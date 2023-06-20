package no.nav.etterlatte.vedtaksvurdering.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.vedtaksvurdering.BeregningOgAvkorting
import org.slf4j.LoggerFactory
import java.util.*

interface BeregningKlient {
    suspend fun hentBeregningOgAvkorting(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        saktype: SakType
    ): BeregningOgAvkorting
}

class BeregningKlientException(override val message: String, override val cause: Throwable) :
    Exception(message, cause)

class BeregningKlientImpl(config: Config, httpClient: HttpClient) : BeregningKlient {
    private val logger = LoggerFactory.getLogger(BeregningKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("beregning.client.id")
    private val resourceUrl = config.getString("beregning.resource.url")

    override suspend fun hentBeregningOgAvkorting(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        saktype: SakType
    ): BeregningOgAvkorting {
        return BeregningOgAvkorting(
            beregning = hentBeregning(behandlingId, brukerTokenInfo),
            avkorting = if (saktype == SakType.OMSTILLINGSSTOENAD) {
                hentAvkorting(behandlingId, brukerTokenInfo)
            } else {
                null
            }
        )
    }

    private suspend fun hentBeregning(behandlingId: UUID, brukerTokenInfo: BrukerTokenInfo): BeregningDTO {
        logger.info("Henter beregning for behandling med behandlingId=$behandlingId")
        try {
            return downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/beregning/$behandlingId"
                    ),
                    brukerTokenInfo = brukerTokenInfo
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage }
                )
        } catch (e: Exception) {
            throw BeregningKlientException(
                "Henting av beregning for behandling med behandlingId=$behandlingId feilet",
                e
            )
        }
    }

    private suspend fun hentAvkorting(behandlingId: UUID, brukerTokenInfo: BrukerTokenInfo): AvkortingDto {
        logger.info("Henter avkorting for behandling med behandlingId=$behandlingId")
        try {
            return downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/beregning/avkorting/$behandlingId"
                    ),
                    brukerTokenInfo = brukerTokenInfo
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage }
                )
        } catch (e: Exception) {
            throw BeregningKlientException(
                "Henting av avkorting for behandling med behandlingId=$behandlingId feilet",
                e
            )
        }
    }
}