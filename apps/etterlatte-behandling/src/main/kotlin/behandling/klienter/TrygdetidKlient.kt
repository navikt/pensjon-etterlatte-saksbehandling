package no.nav.etterlatte.behandling.klienter

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

interface TrygdetidKlient {
    suspend fun kopierTrygdetidFraForrigeBehandling(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    )
}

class TrygdetidKlientImpl(
    config: Config,
    httpClient: HttpClient,
) : TrygdetidKlient {
    private val logger = LoggerFactory.getLogger(TrygdetidKlientImpl::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("trygdetid.client.id")
    private val resourceUrl = config.getString("beregning.resource.url")

    override suspend fun kopierTrygdetidFraForrigeBehandling(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        try {
            logger.info("Kopierer trygdetid fra=$forrigeBehandlingId til=$behandlingId")
            downstreamResourceClient
                .post(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/api/trygdetid_v2/$behandlingId/kopier/$forrigeBehandlingId",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                    postBody = {},
                ).mapBoth(
                    success = { },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: Exception) {
            throw InternfeilException(
                "Kopiering av trygdetid feilet for behandling=$behandlingId",
                e,
            )
        }
    }
}
