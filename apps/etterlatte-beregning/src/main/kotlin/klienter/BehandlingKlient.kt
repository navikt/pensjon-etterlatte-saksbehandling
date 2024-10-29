package no.nav.etterlatte.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.FoersteVirkDto
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.route.BehandlingTilgangsSjekk
import no.nav.etterlatte.libs.ktor.route.SakTilgangsSjekk
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import org.slf4j.LoggerFactory
import java.util.UUID

interface BehandlingKlient :
    BehandlingTilgangsSjekk,
    SakTilgangsSjekk {
    suspend fun hentBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): DetaljertBehandling

    suspend fun hentFoersteVirkningsdato(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): FoersteVirkDto

    suspend fun hentSisteIverksatteBehandling(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): SisteIverksatteBehandling

    suspend fun kanBeregnes(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        commit: Boolean,
    ): Boolean

    suspend fun kanSetteStatusTrygdetidOppdatert(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean

    suspend fun statusTrygdetidOppdatert(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        commit: Boolean,
    ): Boolean

    suspend fun avkort(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        commit: Boolean,
    ): Boolean

    suspend fun opprettOppgave(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
        oppgaveType: OppgaveType,
        merknad: String?,
        frist: Tidspunkt? = null,
        oppgaveKilde: OppgaveKilde,
        referanse: String,
    )
}

class BehandlingKlientException(
    override val message: String,
    override val cause: Throwable,
) : Exception(message, cause)

class BehandlingKlientImpl(
    config: Config,
    httpClient: HttpClient,
) : BehandlingKlient {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    private val tilgangssjekker = Tilgangssjekker(config, httpClient)

    override suspend fun hentBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): DetaljertBehandling {
        logger.info("Henter behandling med behandlingId=$behandlingId")

        return retry<DetaljertBehandling> {
            downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/behandlinger/$behandlingId",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> {
                    throw BehandlingKlientException(
                        "Klarte ikke hente behandling med behandlingId=$behandlingId",
                        it.samlaExceptions(),
                    )
                }
            }
        }
    }

    override suspend fun hentFoersteVirkningsdato(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): FoersteVirkDto {
        logger.info("Henter foersteVirkningsdato med saksId=$sakId")

        return retry<FoersteVirkDto> {
            downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/api/sak/${sakId.sakId}/behandlinger/foerstevirk",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> {
                    throw BehandlingKlientException(
                        "Klarte ikke hente foersteVirkningsdato med saksId=$sakId",
                        it.samlaExceptions(),
                    )
                }
            }
        }
    }

    override suspend fun hentSisteIverksatteBehandling(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): SisteIverksatteBehandling =
        retry<SisteIverksatteBehandling> {
            downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/saker/${sakId.sakId}/behandlinger/sisteIverksatte",
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
                    throw BehandlingKlientException(
                        "Klarte ikke hente siste iverksatte behandling på sak med id=$sakId",
                        it.samlaExceptions(),
                    )
                }
            }
        }

    override suspend fun kanBeregnes(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        commit: Boolean,
    ): Boolean {
        logger.info("Sjekker om behandling med behandlingId=$behandlingId kan beregnes")
        val resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/beregn")

        val response =
            when (commit) {
                false -> downstreamResourceClient.get(resource, brukerTokenInfo)
                true -> downstreamResourceClient.post(resource, brukerTokenInfo, "{}")
            }

        return response.mapBoth(
            success = { true },
            failure = {
                logger.info("Behandling med id $behandlingId kan ikke beregnes, commit=$commit")
                false
            },
        )
    }

    override suspend fun kanSetteStatusTrygdetidOppdatert(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean = statusTrygdetidOppdatert(behandlingId, brukerTokenInfo, false)

    override suspend fun statusTrygdetidOppdatert(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        commit: Boolean,
    ): Boolean {
        logger.info("Sjekker om behandling med behandlingId=$behandlingId kan sette status ${BehandlingStatus.TRYGDETID_OPPDATERT}")
        val resource =
            Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/oppdaterTrygdetid")

        val response =
            when (commit) {
                false -> downstreamResourceClient.get(resource, brukerTokenInfo)
                true -> downstreamResourceClient.post(resource, brukerTokenInfo, "{}")
            }

        return response.mapBoth(
            success = { true },
            failure = {
                logger.info(
                    "Behandling med behandlingId=$behandlingId kan ikke settes " +
                        "status ${BehandlingStatus.TRYGDETID_OPPDATERT}, commit=$commit",
                )
                false
            },
        )
    }

    override suspend fun avkort(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        commit: Boolean,
    ): Boolean {
        logger.info("Sjekker om behandling med behandlingId=$behandlingId kan avkortes")
        val resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/avkort")

        val response =
            when (commit) {
                false -> downstreamResourceClient.get(resource, brukerTokenInfo)
                true -> downstreamResourceClient.post(resource, brukerTokenInfo, "{}")
            }

        return response.mapBoth(
            success = { true },
            failure = {
                logger.info("Behandling med id $behandlingId kan ikke avkortes, commit=$commit")
                false
            },
        )
    }

    override suspend fun opprettOppgave(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
        oppgaveType: OppgaveType,
        merknad: String?,
        frist: Tidspunkt?,
        oppgaveKilde: OppgaveKilde,
        referanse: String,
    ) {
        logger.info("Oppretter oppgave for sakId=$sakId")

        val resource =
            Resource(clientId = clientId, url = "$resourceUrl/oppgaver/sak/${sakId.sakId}/opprett")

        downstreamResourceClient.post(
            resource,
            brukerTokenInfo,
            NyOppgaveDto(
                oppgaveType = oppgaveType,
                merknad = merknad,
                frist = frist,
                oppgaveKilde = oppgaveKilde,
                referanse = referanse,
                saksbehandler = null,
            ),
        )
    }

    override suspend fun harTilgangTilBehandling(
        behandlingId: UUID,
        skrivetilgang: Boolean,
        bruker: Saksbehandler,
    ): Boolean = tilgangssjekker.harTilgangTilBehandling(behandlingId, skrivetilgang, bruker)

    override suspend fun harTilgangTilSak(
        sakId: SakId,
        skrivetilgang: Boolean,
        bruker: Saksbehandler,
    ): Boolean = tilgangssjekker.harTilgangTilSak(sakId, skrivetilgang, bruker)
}
