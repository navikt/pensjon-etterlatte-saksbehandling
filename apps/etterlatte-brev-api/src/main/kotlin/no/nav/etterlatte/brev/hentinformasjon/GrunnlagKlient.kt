package no.nav.etterlatte.brev.hentinformasjon

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class GrunnlagKlientException(override val message: String, override val cause: Throwable) : Exception(message, cause)

class GrunnlagKlient(config: Config, httpClient: HttpClient) {
    private val logger = LoggerFactory.getLogger(GrunnlagKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("grunnlag.client.id")
    private val baseUrl = config.getString("grunnlag.resource.url")

    internal suspend fun hentGrunnlagForSak(
        sakid: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag {
        try {
            logger.info("Henter grunnlag for sak med sakId=$sakid")

            return downstreamResourceClient.get(
                Resource(clientId, "$baseUrl/api/grunnlag/sak/$sakid"),
                brukerTokenInfo,
            ).mapBoth(
                success = { resource -> resource.response.let { deserialize(it.toString()) } },
                failure = { throwableErrorMessage -> throw throwableErrorMessage },
            )
        } catch (e: Exception) {
            throw GrunnlagKlientException("Henting av grunnlag for sak med sakId=$sakid feilet", e)
        }
    }

    internal suspend fun hentGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag {
        try {
            logger.info("Henter grunnlag for behandling (id=$behandlingId)")

            return downstreamResourceClient.get(
                Resource(clientId, "$baseUrl/api/grunnlag/behandling/$behandlingId"),
                brukerTokenInfo,
            ).mapBoth(
                success = { resource -> resource.response.let { deserialize(it.toString()) } },
                failure = { throwableErrorMessage -> throw throwableErrorMessage },
            )
        } catch (e: Exception) {
            throw GrunnlagKlientException("Feil oppsto ved henting av grunnlag for behandling (id=$behandlingId)", e)
        }
    }
}
