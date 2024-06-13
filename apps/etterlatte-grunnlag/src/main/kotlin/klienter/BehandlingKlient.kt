package no.nav.etterlatte.grunnlag.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.route.BehandlingTilgangsSjekk
import no.nav.etterlatte.libs.ktor.route.PersonTilgangsSjekk
import no.nav.etterlatte.libs.ktor.route.SakTilgangsSjekk
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import org.slf4j.LoggerFactory
import java.util.UUID

interface BehandlingKlient :
    BehandlingTilgangsSjekk,
    SakTilgangsSjekk,
    PersonTilgangsSjekk {
    suspend fun hentBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): DetaljertBehandling
}

class BehandlingKlientException(
    override val message: String,
    override val cause: Throwable,
) : Exception(message, cause)

class BehandlingKlientImpl(
    config: Config,
    httpClient: HttpClient,
) : BehandlingKlient {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

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
            throw BehandlingKlientException(
                "Henting av behandling med behandlingId=$behandlingId fra grunnlag feilet",
                e,
            )
        }
    }

    override suspend fun harTilgangTilBehandling(
        behandlingId: UUID,
        skrivetilgang: Boolean,
        bruker: Saksbehandler,
    ): Boolean = tilgangssjekker.harTilgangTilBehandling(behandlingId, skrivetilgang, bruker)

    override suspend fun harTilgangTilSak(
        sakId: Long,
        skrivetilgang: Boolean,
        bruker: Saksbehandler,
    ): Boolean = tilgangssjekker.harTilgangTilSak(sakId, skrivetilgang, bruker)

    override suspend fun harTilgangTilPerson(
        foedselsnummer: Folkeregisteridentifikator,
        skrivetilgang: Boolean,
        bruker: Saksbehandler,
    ): Boolean = tilgangssjekker.harTilgangTilPerson(foedselsnummer, skrivetilgang, bruker)
}
