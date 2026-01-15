package no.nav.etterlatte.vedtaksvurdering.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.oppgave.VedtakEndringDTO
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.route.BehandlingTilgangsSjekk
import no.nav.etterlatte.libs.ktor.route.SakTilgangsSjekk
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse
import org.slf4j.LoggerFactory
import java.util.UUID

interface BehandlingKlient :
    BehandlingTilgangsSjekk,
    SakTilgangsSjekk {
    suspend fun hentBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): DetaljertBehandling

    suspend fun hentSak(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Sak

    suspend fun fattVedtakBehandling(
        brukerTokenInfo: BrukerTokenInfo,
        vedtakEndringDTO: VedtakEndringDTO,
    ): Boolean

    suspend fun underkjennVedtak(
        brukerTokenInfo: BrukerTokenInfo,
        vedtakEndringDTO: VedtakEndringDTO,
    ): Boolean

    suspend fun attesterVedtak(
        brukerTokenInfo: BrukerTokenInfo,
        vedtakEndringDTO: VedtakEndringDTO,
    ): Boolean

    suspend fun kanFatteVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean

    suspend fun kanAttestereVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vedtakHendelse: VedtakHendelse? = null,
    ): Boolean

    suspend fun kanUnderkjenneVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vedtakHendelse: VedtakHendelse? = null,
    ): Boolean

    suspend fun tilSamordning(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vedtakId: Long,
    ): Boolean

    suspend fun samordnet(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vedtakId: Long,
    ): Boolean

    suspend fun iverksett(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vedtakId: Long,
    ): Boolean

    suspend fun hentOppgaverForSak(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<OppgaveIntern>

    suspend fun tildelSaksbehandler(
        oppgaveTilAttestering: OppgaveIntern,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean

    suspend fun hentTilbakekrevingBehandling(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): TilbakekrevingBehandling

    suspend fun hentBeregnetEtteroppgjoerResultat(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregnetEtteroppgjoerResultatDto
}

class BehandlingKlientException(
    override val message: String,
    override val cause: Throwable? = null,
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
        try {
            return downstreamResourceClient
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
        } catch (e: Exception) {
            throw BehandlingKlientException("Henting av behandling med behandlingId=$behandlingId feilet", e)
        }
    }

    override suspend fun hentSak(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Sak {
        logger.info("Henter sak med id $sakId")
        try {
            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/saker/${sakId.sakId}",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: Exception) {
            throw BehandlingKlientException("Henting av sak med id=$sakId feilet", e)
        }
    }

    override suspend fun hentOppgaverForSak(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<OppgaveIntern> {
        logger.info("Henter oppgaver for sak med id $sakId")
        try {
            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/oppgaver/sak/${sakId.sakId}/oppgaver",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: Exception) {
            throw BehandlingKlientException("Henting av sak med id=$sakId feilet", e)
        }
    }

    override suspend fun tildelSaksbehandler(
        oppgaveTilAttestering: OppgaveIntern,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean {
        logger.info("Tildeler oppgaveid ${oppgaveTilAttestering.id} til systembruker")
        val response: Result<Resource, Throwable> =
            downstreamResourceClient
                .post(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/api/oppgaver/${oppgaveTilAttestering.id}/tildel-saksbehandler",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                    postBody = SaksbehandlerEndringDto(saksbehandler = Fagsaksystem.EY.navn),
                )

        return response.mapBoth(
            success = { true },
            failure = { error ->
                logger.error(
                    "Tildeling av $oppgaveTilAttestering til systembruker for attestering feilet",
                    error,
                )
                throw error
            },
        )
    }

    override suspend fun fattVedtakBehandling(
        brukerTokenInfo: BrukerTokenInfo,
        vedtakEndringDTO: VedtakEndringDTO,
    ): Boolean {
        logger.info(
            "Fatter oppgave og commiter sak for behandling" +
                " ${vedtakEndringDTO.sakIdOgReferanse.referanse} " +
                "sakId=${vedtakEndringDTO.sakIdOgReferanse.sakId}",
        )
        val resource = Resource(clientId = clientId, url = "$resourceUrl/fattvedtak")
        val response =
            retryOgPakkUt {
                downstreamResourceClient.post(
                    resource = resource,
                    brukerTokenInfo = brukerTokenInfo,
                    postBody = vedtakEndringDTO,
                )
            }

        return response.mapBoth(
            success = { true },
            failure = { error ->
                logger.error(
                    "Kan ikke fatte oppgaver og commite vedtak av type for behandling " +
                        vedtakEndringDTO.sakIdOgReferanse.referanse,
                )
                throw error
            },
        )
    }

    override suspend fun underkjennVedtak(
        brukerTokenInfo: BrukerTokenInfo,
        vedtakEndringDTO: VedtakEndringDTO,
    ): Boolean {
        logger.info(
            "Underkjenn oppgave og commiter sak for behandling" +
                " ${vedtakEndringDTO.sakIdOgReferanse.referanse} " +
                "sakId=${vedtakEndringDTO.sakIdOgReferanse.sakId}",
        )
        val resource = Resource(clientId = clientId, url = "$resourceUrl/underkjennvedtak")
        val response =
            downstreamResourceClient.post(
                resource = resource,
                brukerTokenInfo = brukerTokenInfo,
                postBody = vedtakEndringDTO,
            )

        return response.mapBoth(
            success = { true },
            failure = { error ->
                logger.error(
                    "Kan ikke underkjenne oppgaver og commite vedtak av type ${vedtakEndringDTO.vedtakType} " +
                        "for behandling ${vedtakEndringDTO.sakIdOgReferanse.referanse}",
                )
                throw error
            },
        )
    }

    override suspend fun attesterVedtak(
        brukerTokenInfo: BrukerTokenInfo,
        vedtakEndringDTO: VedtakEndringDTO,
    ): Boolean {
        logger.info(
            "Attesterer oppgave og commiter sak for behandling" +
                " ${vedtakEndringDTO.sakIdOgReferanse.referanse} " +
                "sakId=${vedtakEndringDTO.sakIdOgReferanse.sakId}",
        )
        val resource = Resource(clientId = clientId, url = "$resourceUrl/attestervedtak")
        val response =
            downstreamResourceClient.post(
                resource = resource,
                brukerTokenInfo = brukerTokenInfo,
                postBody = vedtakEndringDTO,
            )

        return response.mapBoth(
            success = { true },
            failure = { error ->
                logger.error(
                    "Kan ikke attestere oppgaver og commite vedtak av type for behandling " +
                        vedtakEndringDTO.sakIdOgReferanse.referanse,
                )
                throw error
            },
        )
    }

    override suspend fun kanFatteVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean = statussjekkForBehandling(behandlingId, brukerTokenInfo, BehandlingStatus.FATTET_VEDTAK)

    override suspend fun kanAttestereVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vedtakHendelse: VedtakHendelse?,
    ): Boolean = statussjekkForBehandling(behandlingId, brukerTokenInfo, BehandlingStatus.ATTESTERT)

    override suspend fun kanUnderkjenneVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vedtakHendelse: VedtakHendelse?,
    ): Boolean = statussjekkForBehandling(behandlingId, brukerTokenInfo, BehandlingStatus.RETURNERT)

    override suspend fun tilSamordning(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vedtakId: Long,
    ) = downstreamResourceClient
        .post(
            Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/tilsamordning"),
            brukerTokenInfo,
            VedtakHendelse(
                vedtakId = vedtakId,
                inntruffet = Tidspunkt.now(),
            ),
        ).mapBoth({ true }, { false })

    override suspend fun samordnet(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vedtakId: Long,
    ) = downstreamResourceClient
        .post(
            Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/samordnet"),
            brukerTokenInfo,
            VedtakHendelse(
                vedtakId = vedtakId,
                inntruffet = Tidspunkt.now(),
            ),
        ).mapBoth({ true }, { false })

    override suspend fun iverksett(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vedtakId: Long,
    ) = downstreamResourceClient
        .post(
            Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/iverksett"),
            brukerTokenInfo,
            VedtakHendelse(
                vedtakId = vedtakId,
                inntruffet = Tidspunkt.now(),
            ),
        ).mapBoth({ true }, { false })

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

    private suspend fun statussjekkForBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        status: BehandlingStatus,
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
                    it.cause,
                )
                false
            },
        )
    }

    private fun getStatusNavn(status: BehandlingStatus) =
        when (status) {
            BehandlingStatus.FATTET_VEDTAK -> "fatteVedtak"
            BehandlingStatus.ATTESTERT -> "attester"
            BehandlingStatus.RETURNERT -> "returner"
            else -> throw BehandlingKlientException("Ugyldig status ${status.name}")
        }

    override suspend fun hentTilbakekrevingBehandling(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): TilbakekrevingBehandling {
        logger.info("Henter tilbakekreving med tilbakekrevingId=$tilbakekrevingId")
        try {
            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/api/tilbakekreving/$tilbakekrevingId",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: Exception) {
            throw BehandlingKlientException(
                "Henting av tilbakekreving med tilbakekrevingId=$tilbakekrevingId feilet",
                e,
            )
        }
    }

    override suspend fun hentBeregnetEtteroppgjoerResultat(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregnetEtteroppgjoerResultatDto {
        logger.info("Henter beregnet etteroppgjoer resultat for behandlingId=$behandlingId")

        return downstreamResourceClient
            .get(
                Resource(
                    clientId = clientId,
                    url = "$resourceUrl/api/etteroppgjoer/revurdering/$behandlingId/resultat",
                ),
                brukerTokenInfo,
            ).mapBoth(success = { deserialize(it.response.toString()) }, failure = { throw it })
    }
}
