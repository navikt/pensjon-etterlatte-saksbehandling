package no.nav.etterlatte.behandling.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingFattEllerAttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakDto
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakLagretDto
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

interface VedtakKlient {
    suspend fun lagreVedtakTilbakekreving(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: Enhetsnummer,
    ): Long

    suspend fun fattVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: Enhetsnummer,
    ): Long

    suspend fun attesterVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: Enhetsnummer,
    ): TilbakekrevingVedtakLagretDto

    suspend fun underkjennVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Long

    suspend fun lagreVedtakKlage(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto

    suspend fun fattVedtakKlage(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto

    suspend fun attesterVedtakKlage(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto

    suspend fun underkjennVedtakKlage(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto

    suspend fun sakHarLopendeVedtakPaaDato(
        sakId: SakId,
        dato: LocalDate,
        brukerTokenInfo: BrukerTokenInfo,
    ): LoependeYtelseDTO
}

class VedtakKlientException(
    override val message: String,
    override val cause: Throwable,
) : Exception(message, cause)

class VedtakKlientImpl(
    config: Config,
    httpClient: HttpClient,
) : VedtakKlient {
    private val logger = LoggerFactory.getLogger(VedtakKlientImpl::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vedtak.client.id")
    private val resourceUrl = config.getString("vedtak.resource.url")

    override suspend fun lagreVedtakTilbakekreving(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: Enhetsnummer,
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
                ).mapBoth(
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
        enhet: Enhetsnummer,
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
                            enhet = enhet,
                        ),
                ).mapBoth(
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
        enhet: Enhetsnummer,
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
                            enhet = enhet,
                        ),
                ).mapBoth(
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
                ).mapBoth(
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

    override suspend fun lagreVedtakKlage(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto {
        try {
            logger.info(
                "Sender klage som skal lages avvist klage-vedtak for med id=${klage.id} til vedtak",
            )
            return downstreamResourceClient
                .post(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/vedtak/klage/${klage.id}/upsert",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                    postBody = klage,
                ).mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            throw InternfeilException("Lagre vedtak for klage med id=${klage.id} feilet", e)
        }
    }

    override suspend fun fattVedtakKlage(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto {
        try {
            logger.info("Sender klage med id=${klage.id} til vedtak for fatting av vedtak")
            return downstreamResourceClient
                .post(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/vedtak/klage/${klage.id}/fatt",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                    postBody = klage,
                ).mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            throw VedtakKlientException(
                "Fatting av vedtak for klage med id=${klage.id} feilet",
                e,
            )
        }
    }

    override suspend fun attesterVedtakKlage(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto {
        try {
            logger.info("Sender klage med id=${klage.id} til vedtak for attestering")
            return downstreamResourceClient
                .post(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/vedtak/klage/${klage.id}/attester",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                    postBody = klage,
                ).mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            throw VedtakKlientException(
                "Attestering av vedtak for klage med id=${klage.id} feilet",
                e,
            )
        }
    }

    override suspend fun underkjennVedtakKlage(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto {
        try {
            logger.info("Ber om underkjennelse for klage=$klageId til vedtak")
            return downstreamResourceClient
                .post(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/vedtak/klage/$klageId/underkjenn",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                    postBody = { },
                ).mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            throw VedtakKlientException(
                "Underkjennelse av vedtak for klage med id=$klageId feilet",
                e,
            )
        }
    }

    override suspend fun sakHarLopendeVedtakPaaDato(
        sakId: SakId,
        dato: LocalDate,
        brukerTokenInfo: BrukerTokenInfo,
    ): LoependeYtelseDTO {
        try {
            logger.info("Sjekker om sak $sakId er løpende på $dato")
            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/api/vedtak/loepende/${sakId.sakId}?dato=$dato",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            throw VedtakKlientException(
                "Kunne ikke sjekk om sak $sakId har løpende ytelse på dato $dato",
                e,
            )
        }
    }
}
