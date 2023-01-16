package no.nav.etterlatte.behandling.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory
import java.util.*

interface VilkaarsvurderingKlient {
    suspend fun hentVilkaarsvurdering(behandlingId: UUID, accessToken: String): VilkaarsvurderingDto?
}

class VilkaarsvurderingKlientImpl(config: Config, httpClient: HttpClient) : VilkaarsvurderingKlient {
    private val logger = LoggerFactory.getLogger(VilkaarsvurderingKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vilkaarsvurdering.client.id")
    private val resourceUrl = config.getString("vilkaarsvurdering.resource.url")

    override suspend fun hentVilkaarsvurdering(behandlingId: UUID, accessToken: String): VilkaarsvurderingDto? {
        logger.info("Henter vilkaarsvurdering for behandling med id = $behandlingId")
        try {
            val json = downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/vilkaarsvurdering/$behandlingId"
                    ),
                    accessToken = accessToken
                )
                .mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage ->
                        logger.error("Henting av vilkaarsvurdering for en behandling feilet", throwableErrorMessage)
                        null
                    }
                )?.response
            return json?.let { objectMapper.readValue(json.toString()) }
                ?: run { null }
        } catch (e: Exception) {
            logger.error("Henting av behandling ($behandlingId) fra vilkaarsvurdering feilet.", e)
            throw e
        }
    }
}

class VilkaarsvurderingTest : VilkaarsvurderingKlient {
    override suspend fun hentVilkaarsvurdering(behandlingId: UUID, accessToken: String): VilkaarsvurderingDto? {
        TODO("Not yet implemented")
    }
}