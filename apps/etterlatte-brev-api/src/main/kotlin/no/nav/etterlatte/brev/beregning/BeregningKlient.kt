package no.nav.etterlatte.brev.beregning

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.YtelseMedGrunnlagDto
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.Bruker
import org.slf4j.LoggerFactory
import java.util.*

class BeregningKlientException(override val message: String, override val cause: Throwable) : Exception(message, cause)

class BeregningKlient(config: Config, httpClient: HttpClient) {
    private val logger = LoggerFactory.getLogger(BeregningKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("beregning.client.id")
    private val resourceUrl = config.getString("beregning.resource.url")

    suspend fun hentBeregning(behandlingId: UUID, bruker: Bruker): BeregningDTO {
        try {
            logger.info("Henter beregning (behandlingId: $behandlingId)")

            return downstreamResourceClient.get(
                Resource(clientId, "$resourceUrl/api/beregning/$behandlingId"),
                bruker
            ).mapBoth(
                success = { resource -> deserialize(resource.response.toString()) },
                failure = { errorResponse -> throw errorResponse }
            )
        } catch (e: Exception) {
            throw BeregningKlientException(
                "Henting av beregning for behandling med behandlingId=$behandlingId feilet",
                e
            )
        }
    }

    suspend fun hentAvkorting(behandlingId: UUID, bruker: Bruker): AvkortingDto {
        try {
            logger.info("Henter avkorting (behandlingId: $behandlingId)")

            return downstreamResourceClient.get(
                Resource(clientId, "$resourceUrl/api/beregning/avkorting/$behandlingId"),
                bruker
            ).mapBoth(
                success = { resource -> deserialize(resource.response.toString()) },
                failure = { errorResponse -> throw errorResponse }
            )
        } catch (e: Exception) {
            throw BeregningKlientException(
                "Henting av avkorting for behandling med behandlingId=$behandlingId feilet",
                e
            )
        }
    }

    suspend fun hentYtelseMedGrunnlag(behandlingId: UUID, bruker: Bruker): YtelseMedGrunnlagDto {
        try {
            logger.info("Henter utregnet ytelse med grunnlag for behandlingId=$behandlingId")

            return downstreamResourceClient.get(
                Resource(clientId, "$resourceUrl/api/ytelse-med-grunnlag/$behandlingId"),
                bruker
            ).mapBoth(
                success = { resource -> deserialize(resource.response.toString()) },
                failure = { errorResponse -> throw errorResponse }
            )
        } catch (e: Exception) {
            throw BeregningKlientException(
                "Henting av utregnet ytelse med grunnlag for behandling med behandlingId=$behandlingId feilet",
                e
            )
        }
    }
}