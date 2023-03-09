package no.nav.etterlatte.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.TilgangsSjekk
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.Bruker
import no.nav.etterlatte.token.Saksbehandler
import org.slf4j.LoggerFactory
import java.util.*

interface BehandlingKlient : TilgangsSjekk {
    suspend fun hentBehandling(behandlingId: UUID, bruker: Bruker): DetaljertBehandling
    suspend fun sjekkErStrengtFortrolig(fnr: String, bruker: Bruker): Boolean
}

class BehandlingKlientException(override val message: String, override val cause: Throwable) : Exception(message, cause)

class BehandlingKlientImpl(config: Config, httpClient: HttpClient) : BehandlingKlient {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    override suspend fun hentBehandling(behandlingId: UUID, bruker: Bruker): DetaljertBehandling {
        logger.info("Henter behandling med behandlingId=$behandlingId")
        try {
            return downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/behandlinger/$behandlingId"
                    ),
                    bruker = bruker
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage }
                )
        } catch (e: Exception) {
            throw BehandlingKlientException(
                "Henting av behandling med behandlingId=$behandlingId fra grunnlag feilet",
                e
            )
        }
    }

    override suspend fun sjekkErStrengtFortrolig(fnr: String, bruker: Bruker): Boolean {
        try {
            return downstreamResourceClient
                .post(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/personer/sjekkstrengtfortrolig"
                    ),
                    bruker = bruker,
                    postBody = fnr
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage }
                )
        } catch (e: Exception) {
            throw BehandlingKlientException(
                "Sjekking av strengt fortrolig feilet",
                e
            )
        }
    }

    override fun harTilgangTilBehandling(behandlingId: UUID, bruker: Saksbehandler): Boolean {
        return true
    }

    override fun harTilgangTilSak(sakId: Long, bruker: Saksbehandler): Boolean {
        return true
    }

    override fun harTilgangTilPerson(behandlingId: Foedselsnummer, bruker: Saksbehandler): Boolean {
        TODO("Not yet implemented")
    }
}