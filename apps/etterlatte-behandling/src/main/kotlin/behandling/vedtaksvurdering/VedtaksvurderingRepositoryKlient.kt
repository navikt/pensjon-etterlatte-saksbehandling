package no.nav.etterlatte.behandling.vedtaksvurdering

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.AvkortetYtelsePeriode
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtaksvurderingRepositoryKlient(
    config: Config,
    httpClient: HttpClient,
) : VedtaksvurderingRepositoryOperasjoner {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vedtak.client.id")
    private val resourceUrl = config.getString("vedtak.resource.url")

    private val baseUrl get() = "$resourceUrl/intern/vedtak-crud"

    private val systembruker: BrukerTokenInfo = HardkodaSystembruker.river

    override fun opprettVedtak(opprettVedtak: OpprettVedtak): Vedtak =
        runBlocking {
            try {
                logger.info("Oppretter vedtak via klient for behandling=${opprettVedtak.behandlingId}")
                post(url = "$baseUrl/opprett", body = opprettVedtak)
            } catch (e: Exception) {
                throw VedtaksvurderingRepositoryKlientException("Opprett vedtak feilet for behandling=${opprettVedtak.behandlingId}", e)
            }
        }

    override fun oppdaterVedtak(oppdatertVedtak: Vedtak): Vedtak =
        runBlocking {
            try {
                logger.info("Oppdaterer vedtak via klient for behandling=${oppdatertVedtak.behandlingId}")
                put(url = "$baseUrl/oppdater", body = oppdatertVedtak)
            } catch (e: Exception) {
                throw VedtaksvurderingRepositoryKlientException("Oppdater vedtak feilet for behandling=${oppdatertVedtak.behandlingId}", e)
            }
        }

    override fun hentVedtak(vedtakId: Long): Vedtak? =
        runBlocking {
            try {
                logger.info("Henter vedtak via klient med id=$vedtakId")
                getOrNull(url = "$baseUrl/$vedtakId")
            } catch (e: Exception) {
                throw VedtaksvurderingRepositoryKlientException("Hent vedtak feilet for vedtakId=$vedtakId", e)
            }
        }

    override fun hentVedtak(behandlingId: UUID): Vedtak? =
        runBlocking {
            try {
                logger.info("Henter vedtak via klient for behandling=$behandlingId")
                getOrNull(url = "$baseUrl/behandling/$behandlingId")
            } catch (e: Exception) {
                throw VedtaksvurderingRepositoryKlientException("Hent vedtak feilet for behandlingId=$behandlingId", e)
            }
        }

    override fun hentVedtakForSak(sakId: SakId): List<Vedtak> =
        runBlocking {
            try {
                logger.info("Henter vedtak via klient for sak=$sakId")
                get(url = "$baseUrl/sak/${sakId.sakId}")
            } catch (e: Exception) {
                throw VedtaksvurderingRepositoryKlientException("Hent vedtak feilet for sakId=$sakId", e)
            }
        }

    override fun fattVedtak(
        behandlingId: UUID,
        vedtakFattet: VedtakFattet,
    ): Vedtak =
        runBlocking {
            try {
                logger.info("Fatter vedtak via klient for behandling=$behandlingId")
                post(
                    url = "$baseUrl/behandling/$behandlingId/fatt",
                    body = FattVedtakCrudRequest(vedtakFattet = vedtakFattet),
                )
            } catch (e: Exception) {
                throw VedtaksvurderingRepositoryKlientException("Fatt vedtak feilet for behandlingId=$behandlingId", e)
            }
        }

    override fun attesterVedtak(
        behandlingId: UUID,
        attestasjon: Attestasjon,
    ): Vedtak =
        runBlocking {
            try {
                logger.info("Attesterer vedtak via klient for behandling=$behandlingId")
                post(
                    url = "$baseUrl/behandling/$behandlingId/attester",
                    body = AttesterVedtakCrudRequest(attestasjon = attestasjon),
                )
            } catch (e: Exception) {
                throw VedtaksvurderingRepositoryKlientException("Attester vedtak feilet for behandlingId=$behandlingId", e)
            }
        }

    override fun underkjennVedtak(behandlingId: UUID): Vedtak =
        runBlocking {
            try {
                logger.info("Underkjenner vedtak via klient for behandling=$behandlingId")
                post(url = "$baseUrl/behandling/$behandlingId/underkjenn", body = "{}")
            } catch (e: Exception) {
                throw VedtaksvurderingRepositoryKlientException("Underkjenn vedtak feilet for behandlingId=$behandlingId", e)
            }
        }

    override fun tilSamordningVedtak(behandlingId: UUID): Vedtak =
        runBlocking {
            try {
                logger.info("Setter vedtak til samordning via klient for behandling=$behandlingId")
                post(url = "$baseUrl/behandling/$behandlingId/til-samordning", body = "{}")
            } catch (e: Exception) {
                throw VedtaksvurderingRepositoryKlientException("Til samordning feilet for behandlingId=$behandlingId", e)
            }
        }

    override fun samordnetVedtak(behandlingId: UUID): Vedtak =
        runBlocking {
            try {
                logger.info("Setter vedtak samordnet via klient for behandling=$behandlingId")
                post(url = "$baseUrl/behandling/$behandlingId/samordnet", body = "{}")
            } catch (e: Exception) {
                throw VedtaksvurderingRepositoryKlientException("Samordnet vedtak feilet for behandlingId=$behandlingId", e)
            }
        }

    override fun iverksattVedtak(behandlingId: UUID): Vedtak =
        runBlocking {
            try {
                logger.info("Setter vedtak iverksatt via klient for behandling=$behandlingId")
                post(url = "$baseUrl/behandling/$behandlingId/iverksatt", body = "{}")
            } catch (e: Exception) {
                throw VedtaksvurderingRepositoryKlientException("Iverksatt vedtak feilet for behandlingId=$behandlingId", e)
            }
        }

    override fun tilbakestillIkkeIverksatteVedtak(behandlingId: UUID): Vedtak? =
        runBlocking {
            try {
                logger.info("Tilbakestiller vedtak via klient for behandling=$behandlingId")
                patchOrNull(url = "$baseUrl/behandling/$behandlingId/tilbakestill")
            } catch (e: Exception) {
                throw VedtaksvurderingRepositoryKlientException("Tilbakestill vedtak feilet for behandlingId=$behandlingId", e)
            }
        }

    override fun hentFerdigstilteVedtak(
        fnr: Folkeregisteridentifikator,
        sakType: SakType?,
    ): List<Vedtak> =
        runBlocking {
            try {
                logger.info("Henter ferdigstilte vedtak via klient")
                post(
                    url = "$baseUrl/ferdigstilte",
                    body = HentFerdigstilteVedtakRequest(fnr = fnr.value, sakType = sakType),
                )
            } catch (e: Exception) {
                throw VedtaksvurderingRepositoryKlientException("Hent ferdigstilte vedtak feilet", e)
            }
        }

    override fun hentSakIdMedUtbetalingForInntektsaar(
        inntektsaar: Int,
        sakType: SakType?,
    ): List<SakId> =
        runBlocking {
            try {
                logger.info("Henter saker med utbetaling for inntektsaar=$inntektsaar via klient")
                val sakTypeParam = if (sakType != null) "?sakType=${sakType.name}" else ""
                get(url = "$baseUrl/sak-utbetaling/$inntektsaar$sakTypeParam")
            } catch (e: Exception) {
                throw VedtaksvurderingRepositoryKlientException("Hent saker med utbetaling feilet for inntektsaar=$inntektsaar", e)
            }
        }

    override fun harSakUtbetalingForInntektsaar(
        sakId: SakId,
        inntektsaar: Int,
        sakType: SakType,
    ): Boolean =
        runBlocking {
            try {
                logger.info("Sjekker utbetaling for sak=$sakId inntektsaar=$inntektsaar via klient")
                val response: Map<String, Boolean> =
                    get(url = "$baseUrl/sak/${sakId.sakId}/har-utbetaling/$inntektsaar/${sakType.name}")
                response["harUtbetaling"] ?: false
            } catch (e: Exception) {
                throw VedtaksvurderingRepositoryKlientException("Sjekk utbetaling feilet for sakId=$sakId", e)
            }
        }

    override fun hentAvkortetYtelsePerioder(vedtakIds: Set<Long>): List<AvkortetYtelsePeriode> =
        runBlocking {
            try {
                logger.info("Henter avkortet ytelse perioder via klient")
                post(
                    url = "$baseUrl/avkortet-ytelse",
                    body = HentAvkortetYtelsePerioderRequest(vedtakIds = vedtakIds),
                )
            } catch (e: Exception) {
                throw VedtaksvurderingRepositoryKlientException("Hent avkortet ytelse perioder feilet", e)
            }
        }

    override fun lagreManuellBehandlingSamordningsmelding(
        oppdatering: OppdaterSamordningsmelding,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        runBlocking {
            try {
                logger.info("Lagrer manuell samordningsmelding via klient")
                post<Map<String, Any>>(
                    url = "$baseUrl/samordning-manuell",
                    body =
                        LagreManuellSamordningsmeldingCrudRequest(
                            oppdatering = oppdatering,
                            saksbehandlerIdent = brukerTokenInfo.ident(),
                        ),
                    brukerTokenInfoOverride = brukerTokenInfo,
                )
            } catch (e: Exception) {
                throw VedtaksvurderingRepositoryKlientException("Lagre manuell samordningsmelding feilet", e)
            }
        }
    }

    override fun slettManuellBehandlingSamordningsmelding(samId: Long) {
        runBlocking {
            try {
                logger.info("Sletter manuell samordningsmelding via klient for samId=$samId")
                delete(url = "$baseUrl/samordning-manuell/$samId")
            } catch (e: Exception) {
                throw VedtaksvurderingRepositoryKlientException("Slett manuell samordningsmelding feilet for samId=$samId", e)
            }
        }
    }

    override fun tilbakestillTilbakekrevingsvedtak(tilbakekrevingId: UUID) {
        runBlocking {
            try {
                logger.info("Tilbakestiller tilbakekrevingsvedtak via klient for id=$tilbakekrevingId")
                post<Map<String, Any>>(url = "$baseUrl/tilbakestill-tilbakekreving/$tilbakekrevingId", body = "{}")
            } catch (e: Exception) {
                throw VedtaksvurderingRepositoryKlientException("Tilbakestill tilbakekrevingsvedtak feilet for id=$tilbakekrevingId", e)
            }
        }
    }

    private suspend inline fun <reified T> post(
        url: String,
        body: Any,
        brukerTokenInfoOverride: BrukerTokenInfo? = null,
    ): T =
        downstreamResourceClient
            .post(
                resource = Resource(clientId = clientId, url = url),
                brukerTokenInfo = brukerTokenInfoOverride ?: systembruker,
                postBody = body,
            ).mapBoth(
                success = { resource -> objectMapper.readValue(resource.response.toString()) },
                failure = { errorResponse -> throw errorResponse },
            )

    private suspend inline fun <reified T> put(
        url: String,
        body: Any,
    ): T =
        downstreamResourceClient
            .put(
                resource = Resource(clientId = clientId, url = url),
                brukerTokenInfo = systembruker,
                putBody = body,
            ).mapBoth(
                success = { resource -> objectMapper.readValue(resource.response.toString()) },
                failure = { errorResponse -> throw errorResponse },
            )

    private suspend inline fun <reified T> get(url: String): T =
        downstreamResourceClient
            .get(
                resource = Resource(clientId = clientId, url = url),
                brukerTokenInfo = systembruker,
            ).mapBoth(
                success = { resource -> objectMapper.readValue(resource.response.toString()) },
                failure = { errorResponse -> throw errorResponse },
            )

    private suspend inline fun <reified T : Any> getOrNull(url: String): T? =
        try {
            get(url)
        } catch (e: io.ktor.client.plugins.ResponseException) {
            if (e.response.status == io.ktor.http.HttpStatusCode.NotFound) null else throw e
        }

    private suspend inline fun <reified T : Any> patchOrNull(url: String): T? =
        try {
            downstreamResourceClient
                .patch(
                    resource = Resource(clientId = clientId, url = url),
                    brukerTokenInfo = systembruker,
                ).mapBoth(
                    success = { resource -> objectMapper.readValue<T>(resource.response.toString()) },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: io.ktor.client.plugins.ResponseException) {
            if (e.response.status == io.ktor.http.HttpStatusCode.NotFound) null else throw e
        }

    private suspend fun delete(url: String) {
        downstreamResourceClient
            .delete(
                resource = Resource(clientId = clientId, url = url),
                brukerTokenInfo = systembruker,
            ).mapBoth(
                success = { },
                failure = { errorResponse -> throw errorResponse },
            )
    }
}

data class FattVedtakCrudRequest(
    val vedtakFattet: VedtakFattet,
)

data class AttesterVedtakCrudRequest(
    val attestasjon: Attestasjon,
)

data class HentFerdigstilteVedtakRequest(
    val fnr: String,
    val sakType: SakType? = null,
)

data class HentAvkortetYtelsePerioderRequest(
    val vedtakIds: Set<Long>,
)

data class LagreManuellSamordningsmeldingCrudRequest(
    val oppdatering: OppdaterSamordningsmelding,
    val saksbehandlerIdent: String,
)

class VedtaksvurderingRepositoryKlientException(
    override val message: String,
    override val cause: Throwable,
) : Exception(message, cause)
