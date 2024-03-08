package no.nav.etterlatte.brev.behandlingklient

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.oppgave.RedigerFristRequest
import no.nav.etterlatte.libs.common.oppgave.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.oppgave.SettPaaVentRequest
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Systembruker
import java.util.UUID

class BehandlingKlient(config: Config, httpClient: HttpClient) {
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    internal suspend fun hentSak(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): Sak {
        return get(
            url = "$resourceUrl/saker/$sakId",
            onSuccess = { deserialize(it.response!!.toString()) },
            errorMessage = { "Sjekking av tilgang for behandling med id =$sakId feilet" },
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    internal suspend fun hentSisteIverksatteBehandling(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): SisteIverksatteBehandling {
        return get(
            url = "$resourceUrl/saker/$sakId/behandlinger/sisteIverksatte",
            onSuccess = { deserialize(it.response!!.toString()) },
            errorMessage = { "Klarte ikke hente siste iverksatte behandling på sak med id=$sakId" },
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    internal suspend fun hentBrevutfall(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevutfallDto? {
        return get(
            url = "$resourceUrl/api/behandling/$behandlingId/info/brevutfall",
            onSuccess = { it.response?.toString()?.let(::deserialize) },
            brukerTokenInfo = brukerTokenInfo,
            errorMessage = { "Henting av brevutfall for behandling med id=$behandlingId feilet" },
        )
    }

    internal suspend fun hentEtterbetaling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtterbetalingDTO? {
        return get(
            url = "$resourceUrl/api/behandling/$behandlingId/info/etterbetaling",
            onSuccess = { it.response?.toString()?.let(::deserialize) },
            errorMessage = { "Henting av etterbetaling for behandling med id=$behandlingId feilet" },
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    internal suspend fun hentBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): DetaljertBehandling {
        return get(
            url = "$resourceUrl/behandlinger/$behandlingId",
            onSuccess = { deserialize(it.response!!.toString()) },
            errorMessage = { "Klarte ikke hente behandling med id=$behandlingId" },
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    internal suspend fun tildelSaksbehandler(
        oppgaveId: UUID,
        brukerTokenInfo: Systembruker,
    ) = retryOgPakkUt {
        downstreamResourceClient.post(
            resource = Resource(clientId = clientId, url = "$resourceUrl/api/oppgaver/$oppgaveId/tildel-saksbehandler"),
            brukerTokenInfo = brukerTokenInfo,
            postBody = SaksbehandlerEndringDto(brukerTokenInfo.ident()),
        )
    }

    internal suspend fun opprettFrist(
        oppgaveId: UUID,
        frist: Tidspunkt,
        brukerTokenInfo: Systembruker,
    ) = retryOgPakkUt {
        downstreamResourceClient.put(
            resource = Resource(clientId = clientId, url = "$resourceUrl/api/oppgaver/$oppgaveId/frist"),
            brukerTokenInfo = brukerTokenInfo,
            putBody = RedigerFristRequest(frist),
        )
    }

    internal suspend fun settOppgavePaaVent(
        oppgaveId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        merknad: String,
    ) = retryOgPakkUt {
        downstreamResourceClient.post(
            resource = Resource(clientId = clientId, url = "$resourceUrl/api/oppgaver/$oppgaveId/sett-paa-vent"),
            brukerTokenInfo = brukerTokenInfo,
            postBody =
                SettPaaVentRequest(
                    merknad = merknad,
                ),
        )
    }

    internal suspend fun hentVedtaksbehandlingKanRedigeres(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean {
        return get(
            url = "$resourceUrl/vedtaksbehandling/$behandlingId/redigerbar",
            onSuccess = { deserialize(it.response!!.toString()) },
            errorMessage = { "Klarte ikke hente svar på om generisk behandling med id=$behandlingId kan redigeres" },
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    private suspend fun <T> get(
        url: String,
        onSuccess: (Resource) -> T,
        errorMessage: () -> String,
        brukerTokenInfo: BrukerTokenInfo,
    ): T {
        return retry {
            try {
                downstreamResourceClient.get(
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
    }

    suspend fun hentKlage(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Klage {
        return get(
            url = "$resourceUrl/api/klage/$klageId",
            onSuccess = { deserialize(it.response!!.toString()) },
            errorMessage = { "Kunne ikke hente klage med id=$klageId" },
            brukerTokenInfo = brukerTokenInfo,
        )
    }
}

class BehandlingKlientException(override val message: String, override val cause: Throwable? = null) :
    Exception(message, cause)
