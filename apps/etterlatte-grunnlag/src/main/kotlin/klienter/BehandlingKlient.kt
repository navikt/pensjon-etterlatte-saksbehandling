package no.nav.etterlatte.grunnlag.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.ForenkletBehandlingListeWrapper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktor.route.BehandlingTilgangsSjekk
import no.nav.etterlatte.libs.ktor.route.PersonTilgangsSjekk
import no.nav.etterlatte.libs.ktor.route.SakTilgangsSjekk
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Saksbehandler
import org.slf4j.LoggerFactory
import java.util.UUID

interface BehandlingKlient : BehandlingTilgangsSjekk, SakTilgangsSjekk, PersonTilgangsSjekk {
    suspend fun hentBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): DetaljertBehandling

    suspend fun hentBehandlinger(sakId: Long): ForenkletBehandlingListeWrapper
}

class BehandlingKlientException(override val message: String, override val cause: Throwable) : Exception(message, cause)

class BehandlingKlientImpl(config: Config, httpClient: HttpClient, private val httpClientSystembruker: HttpClient) :
    BehandlingKlient {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

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
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: Exception) {
            throw BehandlingKlientException(
                "Henting av behandling med behandlingId=$behandlingId fra grunnlag feilet",
                e,
            )
        }
    }

    override suspend fun hentBehandlinger(sakId: Long): ForenkletBehandlingListeWrapper {
        return httpClientSystembruker.get("$resourceUrl/saker/$sakId/behandlinger").body()
    }

    override suspend fun harTilgangTilBehandling(
        behandlingId: UUID,
        skrivetilgang: Boolean,
        bruker: Saksbehandler,
    ): Boolean {
        try {
            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/tilgang/behandling/$behandlingId?skrivetilgang=$skrivetilgang",
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

    override suspend fun harTilgangTilSak(
        sakId: Long,
        skrivetilgang: Boolean,
        bruker: Saksbehandler,
    ): Boolean {
        try {
            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/tilgang/sak/$sakId?skrivetilgang=$skrivetilgang",
                        ),
                    brukerTokenInfo = bruker,
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: Exception) {
            throw BehandlingKlientException("Sjekking av tilgang for sak feilet", e)
        }
    }

    override suspend fun harTilgangTilPerson(
        foedselsnummer: Folkeregisteridentifikator,
        skrivetilgang: Boolean,
        bruker: Saksbehandler,
    ): Boolean {
        try {
            return downstreamResourceClient
                .post(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/tilgang/person?skrivetilgang=$skrivetilgang",
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
}
