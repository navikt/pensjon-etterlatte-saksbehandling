package no.nav.etterlatte.brev.behandlingklient

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.BehandlingTilgangsSjekk
import no.nav.etterlatte.libs.common.PersonTilgangsSjekk
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.SakTilgangsSjekk
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Saksbehandler
import java.util.UUID

class BehandlingKlient(
    config: Config,
    httpClient: HttpClient,
) : BehandlingTilgangsSjekk, SakTilgangsSjekk, PersonTilgangsSjekk {
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    suspend fun hentSak(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): Sak {
        try {
            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/saker/$sakId",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: Exception) {
            throw BehandlingKlientException("Sjekking av tilgang for behandling feilet", e)
        }
    }

    override suspend fun harTilgangTilBehandling(
        behandlingId: UUID,
        bruker: Saksbehandler,
    ): Boolean {
        try {
            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/tilgang/behandling/$behandlingId",
                        ),
                    brukerTokenInfo = bruker,
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: Exception) {
            throw BehandlingKlientException("Sjekking av tilgang for behandling feilet", e)
        }
    }

    suspend fun hentSisteIverksatteBehandling(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): SisteIverksatteBehandling {
        return retry<SisteIverksatteBehandling> {
            downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/saker/$sakId/behandlinger/sisteIverksatte",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                )
                .mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> {
                    throw BehandlingKlientException(
                        "Klarte ikke hente siste iverksatte behandling på sak med id=$sakId",
                        it.samlaExceptions(),
                    )
                }
            }
        }
    }

    override suspend fun harTilgangTilPerson(
        foedselsnummer: Folkeregisteridentifikator,
        bruker: Saksbehandler,
    ): Boolean {
        try {
            return downstreamResourceClient
                .post(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/tilgang/person",
                        ),
                    brukerTokenInfo = bruker,
                    postBody = foedselsnummer.value,
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: Exception) {
            throw BehandlingKlientException("Sjekking av tilgang for person feilet", e)
        }
    }

    override suspend fun harTilgangTilSak(
        sakId: Long,
        bruker: Saksbehandler,
    ): Boolean {
        try {
            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/tilgang/sak/$sakId",
                        ),
                    brukerTokenInfo = bruker,
                )
                .mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: Exception) {
            throw BehandlingKlientException("Sjekking av tilgang for sak feilet", e)
        }
    }
}

class BehandlingKlientException(override val message: String, override val cause: Throwable? = null) :
    Exception(message, cause)
