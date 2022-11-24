package no.nav.etterlatte.behandling

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.behandling.BehandlingListe
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsOppgave
import no.nav.etterlatte.libs.common.behandling.Grunnlagsendringshendelse
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerRequest
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.typer.LagretHendelser
import no.nav.etterlatte.typer.OppgaveListe
import no.nav.etterlatte.typer.Saker
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.YearMonth

interface EtterlatteBehandling {
    suspend fun hentSakerForPerson(fnr: String, accessToken: String): Saker
    suspend fun hentSaker(accessToken: String): Saker
    suspend fun hentOppgaver(accessToken: String): OppgaveListe
    suspend fun hentBehandlingerForSak(sakId: Int, accessToken: String): BehandlingListe
    suspend fun hentBehandling(behandlingId: String, accessToken: String): Any
    suspend fun avbrytBehandling(behandlingId: String, accessToken: String): Boolean
    suspend fun slettBehandlinger(sakId: Int, accessToken: String): Boolean
    suspend fun hentHendelserForBehandling(behandlingId: String, accessToken: String): LagretHendelser
    suspend fun slettRevurderinger(sakId: Int, accessToken: String): Boolean
    suspend fun opprettManueltOpphoer(manueltOpphoerRequest: ManueltOpphoerRequest, accessToken: String):
        Result<ManueltOpphoerResponse>
    suspend fun hentGrunnlagsendringshendelserForSak(sakId: Int, accessToken: String): GrunnlagsendringshendelseListe
    suspend fun fastsettVirkningstidspunkt(
        behandlingId: String,
        dato: YearMonth,
        accessToken: String
    ): VirkningstidspunktResponse

    suspend fun hentUhaandterteGrunnlagshendelser(accessToken: String): List<GrunnlagsendringsOppgave>
}

data class GrunnlagsendringshendelseListe(val hendelser: List<Grunnlagsendringshendelse>)

class BehandlingKlient(config: Config, httpClient: HttpClient) : EtterlatteBehandling {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    override suspend fun hentSakerForPerson(fnr: String, accessToken: String): Saker {
        try {
            logger.info("Henter saker for en person fra behandling")
            val json = downstreamResourceClient
                .get(
                    Resource(
                        clientId,
                        "$resourceUrl/personer/$fnr/saker"
                    ),
                    accessToken
                ).mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response

            return objectMapper.readValue(json.toString(), Saker::class.java)
        } catch (e: Exception) {
            logger.error("Henting av saker for en person feilet", e)
            throw e
        }
    }

    override suspend fun hentSaker(accessToken: String): Saker {
        try {
            logger.info("Henter alle saker")

            val json = downstreamResourceClient
                .get(
                    Resource(
                        clientId,
                        "$resourceUrl/saker"
                    ),
                    accessToken
                ).mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response

            return objectMapper.readValue(json.toString(), Saker::class.java)
        } catch (e: Exception) {
            logger.error("Henting av saker fra behandling feilet", e)
            throw e
        }
    }

    override suspend fun hentOppgaver(accessToken: String): OppgaveListe {
        try {
            logger.info("Henter alle oppgaver")

            val json = downstreamResourceClient
                .get(
                    Resource(
                        clientId,
                        "$resourceUrl/oppgaver"
                    ),
                    accessToken
                ).mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response

            return objectMapper.readValue(json.toString(), OppgaveListe::class.java)
        } catch (e: Exception) {
            logger.error("Henting av oppgaver fra behandling feilet", e)
            throw e
        }
    }

    override suspend fun hentBehandlingerForSak(sakId: Int, accessToken: String): BehandlingListe {
        logger.info("Henter alle behandlinger i en sak")

        try {
            val json =
                downstreamResourceClient.get(Resource(clientId, "$resourceUrl/sak/$sakId/behandlinger"), accessToken)
                    .mapBoth(
                        success = { json -> json },
                        failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                    ).response
            return objectMapper.readValue(json.toString(), BehandlingListe::class.java)
        } catch (e: Exception) {
            logger.error("Henting av behandlinger feilet", e)
            throw e
        }
    }

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

    override suspend fun avbrytBehandling(behandlingId: String, accessToken: String): Boolean {
        logger.info("Avbryter behandling")
        try {
            downstreamResourceClient.post(
                Resource(
                    clientId,
                    "$resourceUrl/behandlinger/$behandlingId/avbrytbehandling"
                ),
                accessToken,
                ""
            )
                .mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response
            return true
        } catch (e: Exception) {
            logger.error("Avbryt behandling feilet", e)
            throw e
        }
    }

    override suspend fun slettBehandlinger(sakId: Int, accessToken: String): Boolean {
        logger.info("Sletter behandlinger på en sak")
        try {
            downstreamResourceClient.delete(Resource(clientId, "$resourceUrl/sak/$sakId/behandlinger"), accessToken, "")
                .mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response
            return true
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

    override suspend fun slettRevurderinger(sakId: Int, accessToken: String): Boolean {
        logger.info("sletter revurderinger for en sak")
        return try {
            val json =
                downstreamResourceClient.delete(
                    Resource(clientId, "$resourceUrl/behandlinger/revurdering/$sakId"),
                    accessToken,
                    ""
                )
                    .mapBoth(
                        success = { true },
                        failure = { throwableErrorMessage ->
                            throw Error(
                                throwableErrorMessage.message,
                                throwableErrorMessage.throwable
                            )
                        }
                    )
            logger.info("Slettet revurderinger for sak med id $sakId")
            json
        } catch (e: Exception) {
            logger.error("Sletting av revurderinger for sak med id $sakId feilet.", e)
            false
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
            val response: ManueltOpphoerResponse = objectMapper.readValue(json.toString())
            Result.success(response)
        } catch (e: Exception) {
            logger.error("Manuelt opphoer av sak med id ${manueltOpphoerRequest.sak} feilet. ", e)
            Result.failure(e)
        }
    }

    override suspend fun hentGrunnlagsendringshendelserForSak(
        sakId: Int,
        accessToken: String
    ): GrunnlagsendringshendelseListe {
        return try {
            val json = downstreamResourceClient.get(
                Resource(clientId, "$resourceUrl/grunnlagsendringshendelse/$sakId"),
                accessToken
            )
                .mapBoth(
                    success = { json -> json.response },
                    failure = { throwableErrorMessage ->
                        throw Error(throwableErrorMessage.message, throwableErrorMessage.throwable)
                    }
                )
            objectMapper.readValue(json.toString())
        } catch (e: Exception) {
            logger.error("Kunne ikke hente grunnlagsendringshendelser for sak med id $sakId på grunn av feil", e)
            throw e
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

        return objectMapper.readValue(json.toString())
    }

    override suspend fun hentUhaandterteGrunnlagshendelser(accessToken: String): List<GrunnlagsendringsOppgave> {
        val json = downstreamResourceClient.get(
            Resource(clientId, "$resourceUrl/oppgaver/endringshendelser"),
            accessToken
        )
            .mapBoth(
                success = { res -> res.response },
                failure = {
                    throw Error(it.message, it.throwable)
                }
            )
        val res: GrunnlagsendringsoppgaverDto = objectMapper.readValue(json.toString())
        return res.oppgaver
    }
}

data class GrunnlagsendringsoppgaverDto(val oppgaver: List<GrunnlagsendringsOppgave>)

data class ManueltOpphoerResponse(val behandlingId: String)
data class VirkningstidspunktResponse(val dato: YearMonth, val kilde: Kilde) {
    data class Kilde(val ident: String, val tidspunkt: Instant)
}