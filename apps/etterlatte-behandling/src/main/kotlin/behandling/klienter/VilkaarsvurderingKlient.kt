package no.nav.etterlatte.behandling.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
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

class VilkaarsvurderingKlientException(override val message: String, override val cause: Throwable) :
    Exception(message, cause)

class VilkaarsvurderingKlientImpl(config: Config, httpClient: HttpClient) : VilkaarsvurderingKlient {
    private val logger = LoggerFactory.getLogger(VilkaarsvurderingKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vilkaarsvurdering.client.id")
    private val resourceUrl = config.getString("vilkaarsvurdering.resource.url")

    override suspend fun hentVilkaarsvurdering(behandlingId: UUID, accessToken: String): VilkaarsvurderingDto? {
        logger.info("Henter vilkaarsvurdering for behandling med behandlingId=$behandlingId")
        try {
            return downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/vilkaarsvurdering/$behandlingId"
                    ),
                    accessToken = accessToken
                )
                .mapBoth(
                    success = { resource -> resource.response?.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse ->
                        if (errorResponse.downstreamStatusCode == HttpStatusCode.NotFound) {
                            null
                        } else {
                            throw errorResponse
                        }
                    }
                )
        } catch (e: Exception) {
            throw VilkaarsvurderingKlientException(
                "Henting av vilkaarsvurdering for behandling med behandlingId=$behandlingId feilet",
                e
            )
        }
    }
}

class VilkaarsvurderingTest : VilkaarsvurderingKlient {
    override suspend fun hentVilkaarsvurdering(behandlingId: UUID, accessToken: String): VilkaarsvurderingDto? {
        TODO("Not yet implemented")
    }
}