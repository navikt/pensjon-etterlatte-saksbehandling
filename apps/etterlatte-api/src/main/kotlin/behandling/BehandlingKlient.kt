package no.nav.etterlatte.behandling

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerRequest
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.typer.LagretHendelser
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.YearMonth

interface EtterlatteBehandling {
    suspend fun hentBehandling(behandlingId: String, accessToken: String): Any
    suspend fun hentHendelserForBehandling(behandlingId: String, accessToken: String): LagretHendelser
    suspend fun opprettManueltOpphoer(manueltOpphoerRequest: ManueltOpphoerRequest, accessToken: String):
        Result<ManueltOpphoerResponse>

    suspend fun fastsettVirkningstidspunkt(
        behandlingId: String,
        dato: YearMonth,
        accessToken: String
    ): VirkningstidspunktResponse
}

class BehandlingKlient(config: Config, httpClient: HttpClient) : EtterlatteBehandling {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    override suspend fun hentBehandling(behandlingId: String, accessToken: String): DetaljertBehandling {
        logger.info("Henter behandling")
        try {
            val json =
                downstreamResourceClient.get(Resource(clientId, "$resourceUrl/behandlinger/$behandlingId"), accessToken)
                    .mapBoth(
                        success = { json ->
                            json
                        },
                        failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                    ).response

            logger.info("Behandling hentet for behandlingid $behandlingId: $json")
            return objectMapper.readValue(json.toString(), DetaljertBehandling::class.java)
        } catch (e: Exception) {
            logger.error("Henting av behandlinger feilet", e)
            throw e
        }
    }

    override suspend fun hentHendelserForBehandling(behandlingId: String, accessToken: String): LagretHendelser {
        logger.info("Henter hendelser for en behandling")
        try {
            val json =
                downstreamResourceClient.get(
                    Resource(
                        clientId,
                        "$resourceUrl/behandlinger/$behandlingId/hendelser/vedtak"
                    ),
                    accessToken
                )
                    .mapBoth(
                        success = { json -> json },
                        failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                    ).response

            logger.info("Hendelser hentet for behandlingid $behandlingId: $json")
            return objectMapper.readValue(json.toString(), LagretHendelser::class.java)
        } catch (e: Exception) {
            logger.error("Henting av hendelser feilet", e)
            throw e
        }
    }

    override suspend fun opprettManueltOpphoer(
        manueltOpphoerRequest: ManueltOpphoerRequest,
        accessToken: String
    ): Result<ManueltOpphoerResponse> {
        return try {
            val json =
                downstreamResourceClient.post(
                    Resource(
                        clientId,
                        "$resourceUrl/behandlinger/manueltopphoer"
                    ),
                    accessToken,
                    manueltOpphoerRequest
                )
                    .mapBoth(
                        success = { json -> json.response },
                        failure = { throwableErrorMessage ->
                            throw Error(
                                throwableErrorMessage.message,
                                throwableErrorMessage.throwable
                            )
                        }
                    )
            logger.info("Manuelt opphoer av sak med id ${manueltOpphoerRequest.sak} vellykket: $json")
            val response = objectMapper.readValue(json.toString(), ManueltOpphoerResponse::class.java)
            Result.success(response)
        } catch (e: Exception) {
            logger.error("Manuelt opphoer av sak med id ${manueltOpphoerRequest.sak} feilet. ", e)
            Result.failure(e)
        }
    }

    private data class BehandlingVirkningstidspunktRequest(val dato: YearMonth)
    override suspend fun fastsettVirkningstidspunkt(
        behandlingId: String,
        dato: YearMonth,
        accessToken: String
    ): VirkningstidspunktResponse {
        val json = downstreamResourceClient.post(
            Resource(clientId, "$resourceUrl/behandlinger/$behandlingId/virkningstidspunkt"),
            accessToken,
            BehandlingVirkningstidspunktRequest(dato)
        )
            .mapBoth(
                success = { json -> json.response },
                failure = { throwableErrorMessage ->
                    throw Error(throwableErrorMessage.message, throwableErrorMessage.throwable)
                }
            )

        return objectMapper.readValue(json.toString(), VirkningstidspunktResponse::class.java)
    }
}

data class ManueltOpphoerResponse(val behandlingId: String)
data class VirkningstidspunktResponse(val dato: YearMonth, val kilde: Kilde) {
    data class Kilde(val ident: String, val tidspunkt: Instant)
}