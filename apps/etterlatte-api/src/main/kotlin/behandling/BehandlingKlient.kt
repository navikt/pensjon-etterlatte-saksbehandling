package no.nav.etterlatte.behandling

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory


interface EtterlatteBehandling {
    suspend fun hentPerson(fnr: String, accessToken: String): Any?
}

class BehandlingKlient(config: Config) : EtterlatteBehandling {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    private val conf = config
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient)

    @Suppress("UNCHECKED_CAST")
    override suspend fun hentPerson(fnr: String, accessToken: String): String {
        val objectMapper = jacksonObjectMapper()
        try {
            logger.info("Henter saker fra behandling")

            logger.info("Access-token: $accessToken")

            return downstreamResourceClient
                .get(Resource(conf.getString("behandling.client.id"), conf.getString("behandling.resource.url")+"/personer/{fnr}/saker"), accessToken).mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response as String
        } catch (e: Exception) {
            logger.error("Henting av person fra behandling feilet", e)
            throw e
        }
    }
}

