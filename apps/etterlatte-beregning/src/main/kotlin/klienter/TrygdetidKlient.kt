package no.nav.etterlatte.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class TrygdetidKlientException(override val message: String, override val cause: Throwable) :
    Exception(message, cause)

class TrygdetidKlient(config: Config, httpClient: HttpClient) {
    private val logger = LoggerFactory.getLogger(TrygdetidKlient::class.java)
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("trygdetid.client.id")
    private val resourceUrl = config.getString("trygdetid.resource.url")

    suspend fun hentTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): TrygdetidDto? {
        logger.info("Henter trygdetid med behandlingid $behandlingId")
        return retry<TrygdetidDto?> {
            downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/api/trygdetid/$behandlingId",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                )
                .mapBoth(
                    success = { resource -> resource.response?.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> {
                    throw TrygdetidKlientException(
                        "Klarte ikke hente trygdetid for behandling med behandlingId=$behandlingId",
                        it.samlaExceptions(),
                    )
                }
            }
        }
    }

    suspend fun kopierTrygdetid(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        logger.info("Kopierer trygdetid med behandlingid $behandlingId")
        downstreamResourceClient
            .post(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/trygdetid/$behandlingId/kopier/$forrigeBehandlingId",
                    ),
                brukerTokenInfo = brukerTokenInfo,
                {},
            )
            .mapBoth(
                success = { true },
                failure = { throwableErrorMessage -> throw throwableErrorMessage },
            )
    }
}
