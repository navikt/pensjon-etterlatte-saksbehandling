package no.nav.etterlatte.klienter

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.UUID

interface GrunnlagKlient {
    suspend fun hentGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag

    suspend fun aldersovergangMaaned(
        sakId: SakId,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo,
    ): YearMonth
}

class GrunnlagKlientException(
    override val message: String,
    override val cause: Throwable,
) : Exception(message, cause)

class GrunnlagKlientImpl(
    config: Config,
    httpClient: HttpClient,
) : GrunnlagKlient {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    override suspend fun hentGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag {
        logger.info("Henter grunnlag for behandling med id=$behandlingId")

        return retry<Grunnlag> {
            downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/api/grunnlag/behandling/$behandlingId",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> {
                    throw GrunnlagKlientException(
                        "Klarte ikke hente grunnlag for behandling med id=$behandlingId",
                        it.samlaExceptions(),
                    )
                }
            }
        }
    }

    override suspend fun aldersovergangMaaned(
        sakId: SakId,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo,
    ): YearMonth {
        logger.info("Henter måned for aldersovergang for sak=$sakId saktype=${sakType.name}")

        return retry<YearMonth> {
            downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/api/grunnlag/aldersovergang/sak/${sakId.sakId}/${sakType.name}",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> {
                    throw GrunnlagKlientException(
                        "Klarte ikke hente måned for aldersovergang for sak=$sakId saktype=${sakType.name}",
                        it.samlaExceptions(),
                    )
                }
            }
        }
    }
}
