package no.nav.etterlatte.brev.hentinformasjon

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.BeregningsGrunnlagFellesDto
import no.nav.etterlatte.libs.common.beregning.YtelseMedGrunnlagDto
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class BeregningKlientException(override val message: String, override val cause: Throwable) : Exception(message, cause)

class BeregningKlient(config: Config, httpClient: HttpClient) {
    private val logger = LoggerFactory.getLogger(BeregningKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("beregning.client.id")
    private val resourceUrl = config.getString("beregning.resource.url")

    internal suspend fun hentBeregning(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregningDTO? {
        try {
            logger.info("Henter beregning (behandlingId: $behandlingId)")

            return downstreamResourceClient.get(
                Resource(clientId, "$resourceUrl/api/beregning/$behandlingId"),
                brukerTokenInfo,
            ).mapBoth(
                success = { resource -> deserialize(resource.response.toString()) },
                failure = { errorResponse -> throw errorResponse },
            )
        } catch (e: Exception) {
            logger.error(
                "Henting av beregning for behandling med behandlingId=$behandlingId feilet",
                e,
            )
            return null
        }
    }

    internal suspend fun hentBeregningsGrunnlag(
        behandlingId: UUID,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregningsGrunnlagFellesDto? {
        try {
            logger.info("Henter beregningsgrunnlag (behandlingId: $behandlingId)")

            val endepunkt =
                when (sakType) {
                    SakType.BARNEPENSJON -> "barnepensjon"
                    SakType.OMSTILLINGSSTOENAD -> "omstillingstoenad"
                }
            return downstreamResourceClient.get(
                Resource(clientId, "$resourceUrl/api/beregning/beregningsgrunnlag/$behandlingId/$endepunkt"),
                brukerTokenInfo,
            ).mapBoth(
                success = { resource -> deserialize(resource.response.toString()) },
                failure = { errorResponse -> throw errorResponse },
            )
        } catch (e: Exception) {
            logger.error(
                "Henting av beregningsgrunnlag for behandling med behandlingId=$behandlingId feilet",
                e,
            )
            return null
        }
    }

    suspend fun hentYtelseMedGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): YtelseMedGrunnlagDto {
        try {
            logger.info("Henter utregnet ytelse med grunnlag for behandlingId=$behandlingId")

            return downstreamResourceClient.get(
                Resource(clientId, "$resourceUrl/api/beregning/ytelse-med-grunnlag/$behandlingId"),
                brukerTokenInfo,
            ).mapBoth(
                success = { resource -> deserialize(resource.response.toString()) },
                failure = { errorResponse -> throw errorResponse },
            )
        } catch (e: Exception) {
            throw BeregningKlientException(
                "Henting av utregnet ytelse med grunnlag for behandling med behandlingId=$behandlingId feilet",
                e,
            )
        }
    }

    internal suspend fun hentGrunnbeloep(brukerTokenInfo: BrukerTokenInfo): Grunnbeloep {
        try {
            logger.info("Henter gjeldende grunnbeløp")

            return downstreamResourceClient.get(
                Resource(clientId, "$resourceUrl/api/beregning/grunnbeloep"),
                brukerTokenInfo,
            ).mapBoth(
                success = { resource -> deserialize(resource.response.toString()) },
                failure = { errorResponse -> throw errorResponse },
            )
        } catch (e: Exception) {
            throw BeregningKlientException("Henting av grunnbeløp feilet", e)
        }
    }
}
