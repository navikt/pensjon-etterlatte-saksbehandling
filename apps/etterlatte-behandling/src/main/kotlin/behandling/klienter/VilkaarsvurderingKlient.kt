package no.nav.etterlatte.behandling.klienter

import com.github.michaelbull.result.mapError
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.common.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.Pingable
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.ping
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.vilkaarsvurdering.OpprettVilkaarsvurderingFraBehandling
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

interface VilkaarsvurderingKlient : Pingable {
    suspend fun kopierVilkaarsvurdering(
        kopierTilBehandling: UUID,
        kopierFraBehandling: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    )
}

class VilkaarsvurderingKlientImpl(
    config: Config,
    private val httpClient: HttpClient,
) : VilkaarsvurderingKlient {
    private val clientId = config.getString("vilkaarsvurdering.client.id")
    private val resourceUrl = config.getString("vilkaarsvurdering.resource.url")
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun kopierVilkaarsvurdering(
        kopierTilBehandling: UUID,
        kopierFraBehandling: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        downstreamResourceClient
            .post(
                resource = Resource(clientId = clientId, url = "$resourceUrl/api/vilkaarsvurdering/$kopierTilBehandling/kopier"),
                brukerTokenInfo = brukerTokenInfo,
                postBody = OpprettVilkaarsvurderingFraBehandling(forrigeBehandling = kopierFraBehandling),
            ).mapError { error -> throw error }
    }

    override val serviceName: String
        get() = "VilkårsvurderingKlient"
    override val beskrivelse: String
        get() = "Snakker med vilkårsvurdering"
    override val endpoint: String
        get() = this.resourceUrl

    override suspend fun ping(konsument: String?): PingResult =
        httpClient.ping(
            pingUrl = resourceUrl.plus("/health/isready"),
            logger = logger,
            serviceName = serviceName,
            beskrivelse = beskrivelse,
        )
}
