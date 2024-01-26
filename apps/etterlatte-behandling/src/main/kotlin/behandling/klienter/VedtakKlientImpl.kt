package no.nav.etterlatte.behandling.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingFattEllerAttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakDto
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakLagretDto
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

interface VedtakKlient {
    suspend fun lagreVedtakTilbakekreving(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: String,
    ): Long

    suspend fun fattVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: String,
    ): Long

    suspend fun attesterVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: String,
    ): TilbakekrevingVedtakLagretDto

    suspend fun underkjennVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Long
}

class VedtakKlientException(override val message: String, override val cause: Throwable) : Exception(message, cause)

class VedtakKlientImpl(config: Config, httpClient: HttpClient) : VedtakKlient {
    private val logger = LoggerFactory.getLogger(VedtakKlientImpl::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vedtak.client.id")
    private val resourceUrl = config.getString("vedtak.resource.url")

    override suspend fun lagreVedtakTilbakekreving(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: String,
    ): Long {
        try {
            logger.info(
                "Sender tilbakekreving som det skal lagre vedtak for tilbakekreving=${tilbakekrevingBehandling.id} til vedtak",
            )
            return downstreamResourceClient
                .post(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/tilbakekreving/${tilbakekrevingBehandling.id}/lagre-vedtak",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                    postBody =
                        TilbakekrevingVedtakDto(
                            tilbakekrevingId = tilbakekrevingBehandling.id,
                            sakId = tilbakekrevingBehandling.sak.id,
                            sakType = tilbakekrevingBehandling.sak.sakType,
                            soeker = Folkeregisteridentifikator.of(tilbakekrevingBehandling.sak.ident),
                            tilbakekreving = tilbakekrevingBehandling.tilbakekreving.toObjectNode(),
                        ),
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            throw VedtakKlientException(
                "Lagre vedtak for tilbakekreving med id=${tilbakekrevingBehandling.id} feilet",
                e,
            )
        }
    }

    override suspend fun fattVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: String,
    ): Long {
        try {
            logger.info("Sender tilbakekreving som skal fatte vedtak for tilbakekreving=$tilbakekrevingId til vedtak")
            return downstreamResourceClient
                .post(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/tilbakekreving/$tilbakekrevingId/fatt-vedtak",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                    postBody =
                        TilbakekrevingFattEllerAttesterVedtakDto(
                            tilbakekrevingId = tilbakekrevingId,
                            saksbehandler = brukerTokenInfo.ident(),
                            enhet = enhet,
                        ),
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            throw VedtakKlientException(
                "Fatting av vedtak for tilbakekreving med id=$tilbakekrevingId feilet",
                e,
            )
        }
    }

    override suspend fun attesterVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: String,
    ): TilbakekrevingVedtakLagretDto {
        try {
            logger.info("Sender attesteringsinfo for tilbakekreving=$tilbakekrevingId til vedtak")
            return downstreamResourceClient
                .post(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/tilbakekreving/$tilbakekrevingId/attester-vedtak",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                    postBody =
                        TilbakekrevingFattEllerAttesterVedtakDto(
                            tilbakekrevingId = tilbakekrevingId,
                            saksbehandler = brukerTokenInfo.ident(),
                            enhet = enhet,
                        ),
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            throw VedtakKlientException(
                "Attestering av vedtak for tilbakekreving med id=$tilbakekrevingId feilet",
                e,
            )
        }
    }

    override suspend fun underkjennVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Long {
        try {
            logger.info("Ber om underkjennelse for tilbakekreving=$tilbakekrevingId til vedtak")
            return downstreamResourceClient
                .post(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/tilbakekreving/$tilbakekrevingId/underkjenn-vedtak",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                    postBody = tilbakekrevingId,
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            throw VedtakKlientException(
                "Underkjennelse av vedtak for tilbakekreving med id=$tilbakekrevingId feilet",
                e,
            )
        }
    }
}
