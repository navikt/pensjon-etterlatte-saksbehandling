package no.nav.etterlatte.vedtaksvurdering.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.BehandlingTilgangsSjekk
import no.nav.etterlatte.libs.common.SakTilgangsSjekk
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.oppgaveNy.AttesterVedtakOppgave
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
    suspend fun oppgaveAttestering(
        brukerTokenInfo: BrukerTokenInfo,
        attesterVedtakOppgave: AttesterVedtakOppgave
    ): Boolean
    suspend fun fattVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo
    ): Boolean

    suspend fun attester(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vedtakHendelse: VedtakHendelse? = null
    ): Boolean

    suspend fun underkjenn(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vedtakHendelse: VedtakHendelse? = null
    ): Boolean

    suspend fun iverksett(behandlingId: UUID, brukerTokenInfo: BrukerTokenInfo, vedtakId: Long): Boolean
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

    override suspend fun oppgaveAttestering(
        brukerTokenInfo: BrukerTokenInfo,
        attesterVedtakOppgave: AttesterVedtakOppgave
    ): Boolean {
        logger.info(
            "Attesterer oppgave og commiter sak for behandling" +
                " ${attesterVedtakOppgave.attesteringsOppgave.referanse} " +
                "sakId=${attesterVedtakOppgave.attesteringsOppgave.sakId}"
        )
        val resource = Resource(clientId = clientId, url = "$resourceUrl/fattvedtak-behandling")
        val response = downstreamResourceClient.post(
            resource = resource,
            brukerTokenInfo = brukerTokenInfo,
            postBody = attesterVedtakOppgave
        )
        return when (response) {
            is Ok -> true
            is Err -> {
                logger.error(
                    "Kan ikke attestere oppgaver og commite vedtak av type for behandling " +
                        attesterVedtakOppgave.attesteringsOppgave.referanse
                )
                throw response.error
            }
        }
    }

    override suspend fun fattVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo
    ): Boolean {
        return statussjekkForBehandling(behandlingId, brukerTokenInfo, BehandlingStatus.FATTET_VEDTAK)
    }

    override suspend fun attester(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vedtakHendelse: VedtakHendelse?
    ): Boolean {
        return if (vedtakHendelse == null) {
            statussjekkForBehandling(behandlingId, brukerTokenInfo, BehandlingStatus.ATTESTERT)
        } else {
            commitStatussjekkForBehandling(behandlingId, brukerTokenInfo, BehandlingStatus.ATTESTERT, vedtakHendelse)
        }
    }

    override suspend fun underkjenn(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vedtakHendelse: VedtakHendelse?
    ): Boolean {
        return if (vedtakHendelse == null) {
            statussjekkForBehandling(behandlingId, brukerTokenInfo, BehandlingStatus.RETURNERT)
        } else {
            commitStatussjekkForBehandling(behandlingId, brukerTokenInfo, BehandlingStatus.RETURNERT, vedtakHendelse)
        }
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

    private suspend fun commitStatussjekkForBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        status: BehandlingStatus,
        vedtakHendelse: VedtakHendelse
    ): Boolean {
        logger.info("Setter behandling med behandlingId=$behandlingId til status ${status.name} (commit=true)")

        val statusnavn = getStatusNavn(status)
        val resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/$statusnavn")

        val response = downstreamResourceClient.post(
            resource = resource,
            brukerTokenInfo = brukerTokenInfo,
            postBody = vedtakHendelse
        )

        return response.mapBoth(
            success = { true },
            failure = {
                logger.info(
                    "Kan ikke sette status=$status i behandling med behandlingId=$behandlingId (commit=true)",
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