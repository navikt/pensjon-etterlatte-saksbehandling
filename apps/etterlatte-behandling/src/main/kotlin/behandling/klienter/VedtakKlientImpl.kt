package no.nav.etterlatte.behandling.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.InnvilgetPeriodeDto
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingFattEllerAttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

interface VedtakKlient {
    suspend fun lagreVedtakTilbakekreving(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: Enhetsnummer,
    ): VedtakDto

    suspend fun fattVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: Enhetsnummer,
    ): VedtakDto

    suspend fun attesterVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: Enhetsnummer,
    ): VedtakDto

    suspend fun underkjennVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto

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

    suspend fun hentVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto?

    suspend fun hentIverksatteVedtak(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<VedtakSammendragDto>

    suspend fun harSakUtbetalingForInntektsaar(
        sakId: SakId,
        inntektsaar: Int,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean

    suspend fun hentInnvilgedePerioder(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<InnvilgetPeriodeDto>

    suspend fun angreAttesteringTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: Saksbehandler,
        enhet: Enhetsnummer,
    ): VedtakDto
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
    ): VedtakDto {
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
    ): VedtakDto {
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
    ): VedtakDto {
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
    ): VedtakDto {
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

    override suspend fun hentVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto? {
        try {
            logger.info("Henter vedtaksvurdering behandling med behandlingId=$behandlingId")

            return downstreamResourceClient
                .get(
                    Resource(clientId, "$resourceUrl/api/vedtak/$behandlingId"),
                    brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> deserialize(resource.response.toString()) },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (re: ResponseException) {
            if (re.response.status == HttpStatusCode.NotFound) {
                logger.info("Fant ikke vedtak for behandling $behandlingId. Dette er forventa hvis det f.eks. er et varselbrev.")
                return null
            } else {
                logger.error("Ukjent feil ved henting av vedtak for behandling=$behandlingId", re)

                throw ForespoerselException(
                    status = re.response.status.value,
                    code = "UKJENT_FEIL_HENTING_AV_VEDTAKSVURDERING",
                    detail = "Ukjent feil oppsto ved henting av vedtak for behandling",
                    meta = mapOf("behandlingId" to behandlingId),
                )
            }
        }
    }

    override suspend fun hentIverksatteVedtak(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<VedtakSammendragDto> {
        try {
            logger.info("Henter alle iverksatte vedtak for sak=$sakId")

            return downstreamResourceClient
                .get(
                    Resource(clientId, "$resourceUrl/api/vedtak/sak/${sakId.sakId}/iverksatte"),
                    brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> deserialize(resource.response.toString()) },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (re: ResponseException) {
            logger.error("Ukjent feil ved henting av vedtak i sak=$sakId", re)

            throw ForespoerselException(
                status = re.response.status.value,
                code = "UKJENT_FEIL_HENTING_AV_IVERKSATTE_VEDTAK",
                detail = "Ukjent feil oppsto ved henting av iverksatte vedtak",
                meta = mapOf("sakId" to sakId.sakId),
            )
        }
    }

    override suspend fun harSakUtbetalingForInntektsaar(
        sakId: SakId,
        inntektsaar: Int,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean {
        try {
            logger.info("Sjekker om sak $sakId har utbetaling for inntektsaar $inntektsaar")

            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/api/vedtak/sak/$sakId/har-utbetaling-for-inntektsaar/$inntektsaar",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                ).mapBoth(
                    success = { resource ->
                        val response: HarUtbetalingResponse =
                            objectMapper.readValue(resource.response.toString())
                        response.harUtbetaling
                    },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            throw VedtakKlientException(
                "Kunne ikke sjekke utbetaling for sak $sakId og inntektsaar $inntektsaar",
                e,
            )
        }
    }

    override suspend fun hentInnvilgedePerioder(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<InnvilgetPeriodeDto> {
        try {
            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/api/vedtak/sak/$sakId/innvilgede-perioder",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            throw VedtakKlientException(
                "Kunne ikke hente innvilgede perioder i sak med id $sakId",
                e,
            )
        }
    }

    override suspend fun angreAttesteringTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: Saksbehandler,
        enhet: Enhetsnummer,
    ): VedtakDto {
        try {
            return downstreamResourceClient
                .post(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/tilbakekreving/$tilbakekrevingId/tilbakestill-vedtak",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                    postBody = {},
                ).mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            throw VedtakKlientException("Kunne ikke tilbakestille tilbakekrevingsvedtak", e)
        }
    }

    data class HarUtbetalingResponse(
        val harUtbetaling: Boolean,
    )
}
