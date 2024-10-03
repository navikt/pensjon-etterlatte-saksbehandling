package no.nav.etterlatte.brev.behandlingklient

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo

class OppgaveKlient(
    config: Config,
    httpClient: HttpClient,
) {
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    suspend fun opprettOppgave(
        sakId: SakId,
        nyOppgaveDto: NyOppgaveDto,
        brukerTokenInfo: BrukerTokenInfo,
    ): OppgaveIntern =
        retry {
            try {
                downstreamResourceClient
                    .post(
                        resource = Resource(clientId = clientId, url = "$resourceUrl/oppgaver/sak/$sakId/opprett"),
                        brukerTokenInfo = brukerTokenInfo,
                        postBody = nyOppgaveDto,
                    ).mapBoth(
                        success = { resource -> deserialize<OppgaveIntern>(resource.response!!.toString()) },
                        failure = { throwableErrorMessage -> throw throwableErrorMessage },
                    )
            } catch (e: Exception) {
                throw e
            }
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> {
                    throw Exception("Feil oppsto ved opprettelse av oppgave på sak=$sakId", it.samlaExceptions())
                }
            }
        }
}
