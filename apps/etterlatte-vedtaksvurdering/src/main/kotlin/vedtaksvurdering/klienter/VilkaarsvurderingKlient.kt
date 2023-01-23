package no.nav.etterlatte.vedtaksvurdering.klienter

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
    suspend fun hentVilkaarsvurdering(behandlingId: UUID, accessToken: String): VilkaarsvurderingDto
}

class VilkaarsvurderingKlientImpl(config: Config, httpClient: HttpClient) : VilkaarsvurderingKlient {
    private val logger = LoggerFactory.getLogger(VilkaarsvurderingKlientImpl::class.java)
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vilkaarsvurdering.client.id")
    private val resourceUrl = config.getString("vilkaarsvurdering.resource.url")

    override suspend fun hentVilkaarsvurdering(behandlingId: UUID, accessToken: String): VilkaarsvurderingDto {
        logger.info("Henter vilkaarsvurdering med behandlingid $behandlingId")
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
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response
            return objectMapper.readValue(json.toString())
        } catch (e: Exception) {
            logger.error("Henting av vilkårsvurdering ($behandlingId) fra vedtak feilet.")
            throw e
        }
    }
}