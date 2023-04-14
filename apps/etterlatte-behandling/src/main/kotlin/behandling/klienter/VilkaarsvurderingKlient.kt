package no.nav.etterlatte.behandling.klienter

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.Bruker
import org.slf4j.LoggerFactory
import java.util.*

interface VilkaarsvurderingKlient {
    suspend fun kopierVilkaarsvurderingFraForrigeBehandling(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        bruker: Bruker
    )
}

class VilkaarsvurderingKlientException(override val message: String, override val cause: Throwable) :
    Exception(message, cause)

private data class OpprettVilkaarsvurderingFraBehandling(val forrigeBehandling: UUID)
class VilkaarsvurderingKlientImpl(config: Config, httpClient: HttpClient) : VilkaarsvurderingKlient {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vilkaarsvurdering.client.id")
    private val resourceUrl = config.getString("vilkaarsvurdering.resource.url")

    override suspend fun kopierVilkaarsvurderingFraForrigeBehandling(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        bruker: Bruker
    ) {
        try {
            logger.info(
                "Kopierer vilkårsvurdering fra forrige behandling med behandlingId=$forrigeBehandlingId " +
                    "til behandling med behandlingId=$behandlingId"
            )

            downstreamResourceClient.post(
                resource = Resource(
                    clientId = clientId,
                    url = "$resourceUrl/api/vilkaarsvurdering/$behandlingId/kopier"
                ),
                bruker = bruker,
                postBody = OpprettVilkaarsvurderingFraBehandling(forrigeBehandlingId)
            )
        } catch (e: Exception) {
            throw VilkaarsvurderingKlientException(
                "Kopiering av vilkårsvurdering fra forrige behandling med behandlingId=$forrigeBehandlingId " +
                    "til behandling med behandlingId=$behandlingId feilet",
                e
            )
        }
    }
}