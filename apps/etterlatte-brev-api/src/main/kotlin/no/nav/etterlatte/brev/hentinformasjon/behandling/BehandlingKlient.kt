package no.nav.etterlatte.brev.behandlingklient

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.common.behandling.TidligereFamiliepleier
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.OppdaterGrunnlagRequest
import no.nav.etterlatte.libs.common.kodeverk.LandDto
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class BehandlingKlient(
    config: Config,
    httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    internal suspend fun hentSak(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Sak =
        get(
            url = "$resourceUrl/saker/${sakId.sakId}",
            onSuccess = { deserialize(it.response!!.toString()) },
            errorMessage = { "Sjekking av tilgang for behandling med id =$sakId feilet" },
            brukerTokenInfo = brukerTokenInfo,
        )

    internal suspend fun hentSisteIverksatteBehandling(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): SisteIverksatteBehandling =
        get(
            url = "$resourceUrl/saker/${sakId.sakId}/behandlinger/sisteIverksatte",
            onSuccess = { deserialize(it.response!!.toString()) },
            errorMessage = { "Klarte ikke hente siste iverksatte behandling på sak med id=$sakId" },
            brukerTokenInfo = brukerTokenInfo,
        )

    internal suspend fun hentBrevutfall(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevutfallDto? =
        get(
            url = "$resourceUrl/api/behandling/$behandlingId/info/brevutfall",
            onSuccess = { it.response?.toString()?.let(::deserialize) },
            brukerTokenInfo = brukerTokenInfo,
            errorMessage = { "Henting av brevutfall for behandling med id=$behandlingId feilet" },
        )

    internal suspend fun hentEtterbetaling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtterbetalingDTO? =
        get(
            url = "$resourceUrl/api/behandling/$behandlingId/info/etterbetaling",
            onSuccess = { it.response?.toString()?.let(::deserialize) },
            errorMessage = { "Henting av etterbetaling for behandling med id=$behandlingId feilet" },
            brukerTokenInfo = brukerTokenInfo,
        )

    internal suspend fun hentBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): DetaljertBehandling =
        get(
            url = "$resourceUrl/behandlinger/$behandlingId",
            onSuccess = { deserialize(it.response!!.toString()) },
            errorMessage = { "Klarte ikke hente behandling med id=$behandlingId" },
            brukerTokenInfo = brukerTokenInfo,
        )

    internal suspend fun hentBehandlingMedBrevKanRedigeres(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean =
        get(
            url = "$resourceUrl/behandling-med-brev/$behandlingId/redigerbar",
            onSuccess = { deserialize(it.response!!.toString()) },
            errorMessage = { "Klarte ikke hente svar på om behandling med brev med id=$behandlingId kan redigeres" },
            brukerTokenInfo = brukerTokenInfo,
        )

    internal suspend fun hentTidligereFamiliepleier(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): TidligereFamiliepleier? =
        get(
            url = "$resourceUrl/api/behandling/$behandlingId/tidligere-familiepleier",
            onSuccess = { it.response?.toString()?.let(::deserialize) },
            errorMessage = { "Klarte ikke hente svar på om tidligere familiepleier for behandling med id=$behandlingId" },
            brukerTokenInfo = brukerTokenInfo,
        )

    private suspend fun <T> get(
        url: String,
        onSuccess: (Resource) -> T,
        errorMessage: () -> String,
        brukerTokenInfo: BrukerTokenInfo,
    ): T =
        retry {
            try {
                downstreamResourceClient
                    .get(
                        resource = Resource(clientId = clientId, url = url),
                        brukerTokenInfo = brukerTokenInfo,
                    ).mapBoth(
                        success = onSuccess,
                        failure = { throwableErrorMessage -> throw throwableErrorMessage },
                    )
            } catch (e: Exception) {
                throw e
            }
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> {
                    throw BehandlingKlientException(errorMessage(), it.samlaExceptions())
                }
            }
        }

    internal suspend fun hentKlage(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Klage =
        get(
            url = "$resourceUrl/api/klage/$klageId",
            onSuccess = { deserialize(it.response!!.toString()) },
            errorMessage = { "Kunne ikke hente klage med id=$klageId" },
            brukerTokenInfo = brukerTokenInfo,
        )

    internal suspend fun hentLand(brukerTokenInfo: BrukerTokenInfo): List<LandDto> =
        get(
            url = "$resourceUrl/api/kodeverk/land",
            onSuccess = { deserialize(it.response.toString()) },
            errorMessage = { "Kunne ikke hente land fra kodeverk" },
            brukerTokenInfo = brukerTokenInfo,
        )

    internal suspend fun hentGrunnlagForSak(
        sakid: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag {
        try {
            logger.info("Henter grunnlag for sak med sakId=$sakid")

            return downstreamResourceClient
                .get(
                    Resource(clientId, "$resourceUrl/api/grunnlag/sak/${sakid.sakId}"),
                    brukerTokenInfo,
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: ResponseException) {
            logger.error("Henting av grunnlag for sak med sakId=$sakid feilet", e)

            throw ForespoerselException(
                status = e.response.status.value,
                code = "UKJENT_FEIL_HENT_GRUNNLAG",
                detail = "Henting av grunnlag for sak feilet",
            )
        }
    }

    internal suspend fun hentGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag {
        try {
            logger.info("Henter grunnlag for behandling med id=$behandlingId")

            return downstreamResourceClient
                .get(
                    Resource(clientId, "$resourceUrl/api/grunnlag/behandling/$behandlingId"),
                    brukerTokenInfo,
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: ResponseException) {
            logger.error("Henting av grunnlag for behandling med id=$behandlingId feilet", e)

            throw ForespoerselException(
                status = e.response.status.value,
                code = "UKJENT_FEIL_HENT_GRUNNLAG",
                detail = "Henting av grunnlag for behandling feilet",
            )
        }
    }

    internal suspend fun oppdaterGrunnlagForSak(
        sak: Sak,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean {
        try {
            logger.info("Oppdaterer grunnlag for sak med id=${sak.id}")

            return downstreamResourceClient
                .post(
                    Resource(clientId, "$resourceUrl/api/grunnlag/sak/${sak.id}/oppdater-grunnlag"),
                    brukerTokenInfo,
                    OppdaterGrunnlagRequest(sak.id, sak.sakType),
                ).mapBoth(
                    success = { true },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: ResponseException) {
            logger.error("Oppdatering av grunnlag for sak med id=${sak.id} feilet", e)

            throw ForespoerselException(
                status = e.response.status.value,
                code = "UKJENT_FEIL_OPPDATER_GRUNNLAG",
                detail = "Oppdatering av grunnlag for sak feilet id: ${sak.id}",
            )
        }
    }

    internal suspend fun finnesGrunnlagForSak(
        sakId: SakId,
        bruker: BrukerTokenInfo,
    ): Boolean {
        try {
            logger.info("Oppdaterer grunnlag for sak med id=$sakId")

            return downstreamResourceClient
                .get(
                    Resource(clientId, "$resourceUrl/api/grunnlag/sak/$sakId/grunnlag-finnes"),
                    bruker,
                ).mapBoth(
                    success = { deserialize(it.response!!.toString()) },
                    failure = { throw it },
                )
        } catch (e: ResponseException) {
            logger.error("Sjekk på om grunnlag finnes feilet (sakId=$sakId)", e)

            throw ForespoerselException(
                status = e.response.status.value,
                code = "UKJENT_FEIL_FINNES_GRUNNLAG",
                detail = "Sjekk på om grunnlag finnes feilet",
            )
        }
    }
}

class BehandlingKlientException(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause)
