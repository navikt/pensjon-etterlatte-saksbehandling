package no.nav.etterlatte.behandling.klienter

import com.github.michaelbull.result.mapError
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

// <Ping interface også?
interface VilkaarsvurderingKlient {
    suspend fun kopierVilkaarsvurdering(
        kopierTilBehandling: UUID,
        kopierFraBehandling: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    )
}

class VilkaarsvurderingKlientImpl(
    config: Config,
    httpClient: HttpClient,
) : VilkaarsvurderingKlient {
    private val clientId = config.getString("vilkaarsvurdering.client.id")
    private val resourceUrl = config.getString("vilkaarsvurdering.resource.url")
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    override suspend fun kopierVilkaarsvurdering(
        kopierTilBehandling: UUID,
        kopierFraBehandling: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        downstreamResourceClient
            .post(
                resource = Resource(clientId = clientId, url = "$resourceUrl/api/vilkaarsvurdering/$kopierTilBehandling/kopier"),
                brukerTokenInfo = brukerTokenInfo,
                postBody = mapOf("forrigeBehandling" to kopierFraBehandling).toJson(),
            ).mapError { error -> throw error }
    }
}
