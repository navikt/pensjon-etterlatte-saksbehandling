package no.nav.etterlatte.vedtaksvurdering.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.BehandlingTilgangsSjekk
import no.nav.etterlatte.libs.common.SakTilgangsSjekk
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveListe
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveNy
import no.nav.etterlatte.libs.common.oppgaveNy.VedtakEndringDTO
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Saksbehandler
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse
import org.slf4j.LoggerFactory
import java.util.*

interface BehandlingKlient : BehandlingTilgangsSjekk, SakTilgangsSjekk {
    suspend fun hentBehandling(behandlingId: UUID, brukerTokenInfo: BrukerTokenInfo): DetaljertBehandling
    suspend fun hentSak(sakId: Long, brukerTokenInfo: BrukerTokenInfo): Sak
    suspend fun fattVedtakBehandling(
        brukerTokenInfo: BrukerTokenInfo,
        vedtakEndringDTO: VedtakEndringDTO
    ): Boolean

    suspend fun underkjennVedtak(
        brukerTokenInfo: BrukerTokenInfo,
        vedtakEndringDTO: VedtakEndringDTO
    ): Boolean

    suspend fun attesterVedtak(
        brukerTokenInfo: BrukerTokenInfo,
        vedtakEndringDTO: VedtakEndringDTO
    ): Boolean

    suspend fun kanFatteVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo
    ): Boolean

    suspend fun kanAttestereVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vedtakHendelse: VedtakHendelse? = null
    ): Boolean

    suspend fun kanUnderkjenneVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vedtakHendelse: VedtakHendelse? = null
    ): Boolean

    suspend fun iverksett(behandlingId: UUID, brukerTokenInfo: BrukerTokenInfo, vedtakId: Long): Boolean

    suspend fun hentOppgaverForSak(sakId: Long, brukerTokenInfo: BrukerTokenInfo): OppgaveListe
    suspend fun tildelSaksbehandler(oppgaveTilAttestering: OppgaveNy, brukerTokenInfo: BrukerTokenInfo): Boolean
}

class BehandlingKlientException(override val message: String, override val cause: Throwable? = null) :
    Exception(message, cause)

class BehandlingKlientImpl(config: Config, httpClient: HttpClient) : BehandlingKlient {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    override suspend fun hentBehandling(behandlingId: UUID, brukerTokenInfo: BrukerTokenInfo): DetaljertBehandling {
        logger.info("Henter behandling med behandlingId=$behandlingId")
        try {
            return downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/behandlinger/$behandlingId"
                    ),
                    brukerTokenInfo = brukerTokenInfo
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage }
                )
        } catch (e: Exception) {
            throw BehandlingKlientException("Henting av behandling med behandlingId=$behandlingId feilet", e)
        }
    }

    override suspend fun hentSak(sakId: Long, brukerTokenInfo: BrukerTokenInfo): Sak {
        logger.info("Henter sak med id $sakId")
        try {
            return downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/saker/$sakId"
                    ),
                    brukerTokenInfo = brukerTokenInfo
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage }
                )
        } catch (e: Exception) {
            throw BehandlingKlientException("Henting av sak med id=$sakId feilet", e)
        }
    }

    override suspend fun hentOppgaverForSak(sakId: Long, brukerTokenInfo: BrukerTokenInfo): OppgaveListe {
        logger.info("Henter oppgaver for sak med id $sakId")
        try {
            return downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/sak/$sakId/oppgaver"
                    ),
                    brukerTokenInfo = brukerTokenInfo
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage }
                )
        } catch (e: Exception) {
            throw BehandlingKlientException("Henting av sak med id=$sakId feilet", e)
        }
    }

    override suspend fun tildelSaksbehandler(
        oppgaveTilAttestering: OppgaveNy,
        brukerTokenInfo: BrukerTokenInfo
    ): Boolean {
        logger.info("Tildeler oppgave $oppgaveTilAttestering til systembruker")
        val response = downstreamResourceClient
            .post(
                resource = Resource(
                    clientId = clientId,
                    url = "$resourceUrl/nyeoppgaver/${oppgaveTilAttestering.id}/tildel-saksbehandler"
                ),
                brukerTokenInfo = brukerTokenInfo,
                postBody = {}
            )

        return when (response) {
            is Ok -> true
            is Err -> {
                logger.error(
                    "Tildeling av $oppgaveTilAttestering til systembruker for attestering feilet",
                    response.error
                )
                throw response.error
            }
        }
    }

    override suspend fun fattVedtakBehandling(
        brukerTokenInfo: BrukerTokenInfo,
        vedtakEndringDTO: VedtakEndringDTO
    ): Boolean {
        logger.info(
            "Fatter oppgave og commiter sak for behandling" +
                " ${vedtakEndringDTO.vedtakOppgaveDTO.referanse} " +
                "sakId=${vedtakEndringDTO.vedtakOppgaveDTO.sakId}"
        )
        val resource = Resource(clientId = clientId, url = "$resourceUrl/fattvedtak")
        val response = downstreamResourceClient.post(
            resource = resource,
            brukerTokenInfo = brukerTokenInfo,
            postBody = vedtakEndringDTO
        )
        return when (response) {
            is Ok -> true
            is Err -> {
                logger.error(
                    "Kan ikke fatte oppgaver og commite vedtak av type for behandling " +
                        vedtakEndringDTO.vedtakOppgaveDTO.referanse
                )
                throw response.error
            }
        }
    }

    override suspend fun underkjennVedtak(
        brukerTokenInfo: BrukerTokenInfo,
        vedtakEndringDTO: VedtakEndringDTO
    ): Boolean {
        logger.info(
            "Underkjenn oppgave og commiter sak for behandling" +
                " ${vedtakEndringDTO.vedtakOppgaveDTO.referanse} " +
                "sakId=${vedtakEndringDTO.vedtakOppgaveDTO.sakId}"
        )
        val resource = Resource(clientId = clientId, url = "$resourceUrl/underkjennvedtak")
        val response = downstreamResourceClient.post(
            resource = resource,
            brukerTokenInfo = brukerTokenInfo,
            postBody = vedtakEndringDTO
        )
        return when (response) {
            is Ok -> true
            is Err -> {
                logger.error(
                    "Kan ikke underkjenne oppgaver og commite vedtak av type for behandling " +
                        vedtakEndringDTO.vedtakOppgaveDTO.referanse
                )
                throw response.error
            }
        }
    }

    override suspend fun attesterVedtak(
        brukerTokenInfo: BrukerTokenInfo,
        vedtakEndringDTO: VedtakEndringDTO
    ): Boolean {
        logger.info(
            "Attesterer oppgave og commiter sak for behandling" +
                " ${vedtakEndringDTO.vedtakOppgaveDTO.referanse} " +
                "sakId=${vedtakEndringDTO.vedtakOppgaveDTO.sakId}"
        )
        val resource = Resource(clientId = clientId, url = "$resourceUrl/attestervedtak")
        val response = downstreamResourceClient.post(
            resource = resource,
            brukerTokenInfo = brukerTokenInfo,
            postBody = vedtakEndringDTO
        )
        return when (response) {
            is Ok -> true
            is Err -> {
                logger.error(
                    "Kan ikke attestere oppgaver og commite vedtak av type for behandling " +
                        vedtakEndringDTO.vedtakOppgaveDTO.referanse
                )
                throw response.error
            }
        }
    }

    override suspend fun kanFatteVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo
    ): Boolean {
        return statussjekkForBehandling(behandlingId, brukerTokenInfo, BehandlingStatus.FATTET_VEDTAK)
    }

    override suspend fun kanAttestereVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vedtakHendelse: VedtakHendelse?
    ): Boolean {
        return statussjekkForBehandling(behandlingId, brukerTokenInfo, BehandlingStatus.ATTESTERT)
    }

    override suspend fun kanUnderkjenneVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vedtakHendelse: VedtakHendelse?
    ): Boolean {
        return statussjekkForBehandling(behandlingId, brukerTokenInfo, BehandlingStatus.RETURNERT)
    }

    override suspend fun iverksett(behandlingId: UUID, brukerTokenInfo: BrukerTokenInfo, vedtakId: Long) =
        downstreamResourceClient.post(
            Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/iverksett"),
            brukerTokenInfo,
            VedtakHendelse(
                vedtakId = vedtakId,
                inntruffet = Tidspunkt.now()
            )
        ).mapBoth({ true }, { false })

    override suspend fun harTilgangTilBehandling(behandlingId: UUID, bruker: Saksbehandler): Boolean {
        try {
            return downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/tilgang/behandling/$behandlingId"
                    ),
                    brukerTokenInfo = bruker
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage }
                )
        } catch (e: Exception) {
            throw BehandlingKlientException("Sjekking av tilgang for behandling feilet", e)
        }
    }

    override suspend fun harTilgangTilSak(sakId: Long, bruker: Saksbehandler): Boolean {
        try {
            return downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/tilgang/sak/$sakId"
                    ),
                    brukerTokenInfo = bruker
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage }
                )
        } catch (e: Exception) {
            throw BehandlingKlientException("Sjekking av tilgang for sak feilet", e)
        }
    }

    private suspend fun statussjekkForBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        status: BehandlingStatus
    ): Boolean {
        logger.info("Sjekker behandling med behandlingId=$behandlingId til status ${status.name} (commit=false)")

        val statusnavn = getStatusNavn(status)
        val resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/$statusnavn")

        val response = downstreamResourceClient.get(resource = resource, brukerTokenInfo = brukerTokenInfo)

        return response.mapBoth(
            success = { true },
            failure = {
                logger.info(
                    "Kan ikke sjekke status=$status i behandling med behandlingId=$behandlingId (commit=false)",
                    it.throwable
                )
                false
            }
        )
    }

    private fun getStatusNavn(status: BehandlingStatus) =
        when (status) {
            BehandlingStatus.FATTET_VEDTAK -> "fatteVedtak"
            BehandlingStatus.ATTESTERT -> "attester"
            BehandlingStatus.RETURNERT -> "returner"
            else -> throw BehandlingKlientException("Ugyldig status ${status.name}")
        }
}