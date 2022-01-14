package no.nav.etterlatte.behandling

import com.github.michaelbull.result.mapBoth

import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.Configuration
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory

interface EtterlatteBehandling {
    suspend fun hentPerson(fnr: String, accessToken: String): Any?
}

class BehandlingKlient() : EtterlatteBehandling {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    private val config = Configuration()
    private val azureAdClient = AzureAdClient(config.azureAd)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient)

    @Suppress("UNCHECKED_CAST")
    override suspend fun hentPerson(fnr: String, accessToken: String): List<Sak> =
        try {
            logger.info("Henter saker fra behandling")

             downstreamResourceClient
                .get(Resource(config.downstream.clientId, "personer/{fnr}/saker"), accessToken).mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response as List<Sak>

        } catch (e: Exception) {
            logger.error("Henting av person fra behandling feilet", e)
            throw e
        }
}

