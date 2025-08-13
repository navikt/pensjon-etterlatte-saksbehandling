package no.nav.etterlatte.brev.hentinformasjon.beregning

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.BeregningOgAvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregningsGrunnlagFellesDto
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class BeregningKlient(
    config: Config,
    httpClient: HttpClient,
) {
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

            return downstreamResourceClient
                .get(
                    Resource(clientId, "$resourceUrl/api/beregning/$behandlingId"),
                    brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> deserialize(resource.response.toString()) },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            if (e is ResponseException && e.response.status == HttpStatusCode.NotFound) {
                logger.info("Fant ikke beregning for behandling med behandlingId=$behandlingId", e)
                return null
            }
            logger.error("Henting av beregning for behandling med behandlingId=$behandlingId feilet", e)
            throw e
        }
    }

    internal suspend fun hentBeregningsGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregningsGrunnlagFellesDto? {
        try {
            logger.info("Henter beregningsgrunnlag (behandlingId: $behandlingId)")

            return downstreamResourceClient
                .get(
                    Resource(clientId, "$resourceUrl/api/beregning/beregningsgrunnlag/$behandlingId"),
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

    internal suspend fun hentBeregningOgAvkorting(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregningOgAvkortingDto {
        try {
            logger.info("Henter beregning og avkorting for behandlingId=$behandlingId")

            return downstreamResourceClient
                .get(
                    Resource(clientId, "$resourceUrl/api/beregning/ytelse-med-grunnlag/$behandlingId"),
                    brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> deserialize(resource.response.toString()) },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (re: ResponseException) {
            logger.error(
                "Henting av beregning og avkorting for behandling med behandlingId=$behandlingId feilet",
                re,
            )

            throw ForespoerselException(
                status = re.response.status.value,
                code = "FEIL_HENT_BEREGNING_AVKORTING",
                detail = "Henting av beregning og avkorting for behandling med behandlingId=$behandlingId feilet",
            )
        }
    }

    internal suspend fun hentGrunnbeloep(brukerTokenInfo: BrukerTokenInfo): Grunnbeloep {
        try {
            logger.info("Henter gjeldende grunnbeløp")

            return downstreamResourceClient
                .get(
                    Resource(clientId, "$resourceUrl/api/beregning/grunnbeloep"),
                    brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> deserialize(resource.response.toString()) },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (re: ResponseException) {
            logger.error("Henting av grunnbeløp feilet", re)

            throw ForespoerselException(
                status = re.response.status.value,
                code = "FEIL_HENT_GRUNNBELOEP",
                detail = "Henting av grunnbeløp feilet",
            )
        }
    }
}
