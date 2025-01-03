package no.nav.etterlatte.behandling.klienter

import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoRequest
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoResponse
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

interface BeregningKlient {
    suspend fun slettAvkorting(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    )

    suspend fun harOverstyrt(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean

    suspend fun inntektsjusteringAvkortingInfoSjekk(
        sakId: SakId,
        aar: Int,
        sisteBehandling: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): InntektsjusteringAvkortingInfoResponse
}

class BeregningKlientImpl(
    config: Config,
    httpClient: HttpClient,
) : BeregningKlient {
    private val logger = LoggerFactory.getLogger(BeregningKlientImpl::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("beregning.client.id")
    private val resourceUrl = config.getString("beregning.resource.url")

    override suspend fun slettAvkorting(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        logger.info("Sletter avkorting for behandling id=$behandlingId")
        downstreamResourceClient
            .delete(
                resource = Resource(clientId = clientId, url = "$resourceUrl/api/beregning/avkorting/$behandlingId"),
                brukerTokenInfo = brukerTokenInfo,
                postBody = "",
            ).mapError { error -> throw error }
    }

    override suspend fun harOverstyrt(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean {
        logger.info("Henter overstyrt beregning for behandling id=$behandlingId")
        return downstreamResourceClient
            .get(
                resource = Resource(clientId = clientId, url = "$resourceUrl/api/beregning/$behandlingId/overstyrt"),
                brukerTokenInfo = brukerTokenInfo,
            ).mapBoth(
                success = { resource -> resource.response != null },
                failure = { throwableErrorMessage -> throw throwableErrorMessage },
            )
    }

    override suspend fun inntektsjusteringAvkortingInfoSjekk(
        sakId: SakId,
        aar: Int,
        sisteBehandling: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): InntektsjusteringAvkortingInfoResponse {
        logger.info("Sjekker om sakId=$sakId har inntekt for aar=$aar")
        val response =
            downstreamResourceClient.post(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/beregning/avkorting/aarlig-inntektsjustering-sjekk",
                    ),
                brukerTokenInfo = brukerTokenInfo,
                postBody =
                    InntektsjusteringAvkortingInfoRequest(
                        sakId = sakId,
                        aar = aar,
                        sisteBehandling = sisteBehandling,
                    ),
            )

        return response.mapBoth(
            success = { resource -> deserialize(resource.response.toString()) },
            failure = {
                throw InternfeilException(
                    "Klarte ikke sjekke om sakId=$sakId har inntekt for aar=$aar, ${it.cause}",
                )
            },
        )
    }
}
