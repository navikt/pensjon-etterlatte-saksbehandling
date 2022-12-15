package no.nav.etterlatte.behandling

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory
import java.util.UUID

interface BeregningKlient {
    suspend fun hentBeregning(behandlingId: UUID, accessToken: String): BeregningDTO?
}

class BeregningKlientImpl(config: Config, httpClient: HttpClient) : BeregningKlient {
    private val logger = LoggerFactory.getLogger(BeregningKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("beregning.client.id")
    private val resourceUrl = config.getString("beregning.resource.url")

    override suspend fun hentBeregning(behandlingId: UUID, accessToken: String): BeregningDTO? {
        logger.info("Henter beregning for behandling med id = $behandlingId")
        try {
            val json = downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/beregning/$behandlingId"
                    ),
                    accessToken = accessToken
                )
                .mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage ->
                        logger.warn("Henting av beregning for en behandling feilet", throwableErrorMessage)
                        null
                    }
                )?.response
            return json?.let { objectMapper.readValue(json.toString()) }
                ?: run { null }
        } catch (e: Exception) {
            logger.error("Henting av behandling ($behandlingId) fra beregning feilet.", e)
            throw e
        }
    }
}