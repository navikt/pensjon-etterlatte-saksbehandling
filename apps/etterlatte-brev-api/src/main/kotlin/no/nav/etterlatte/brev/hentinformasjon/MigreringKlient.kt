package no.nav.etterlatte.brev.hentinformasjon

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.token.BrukerTokenInfo
import java.util.UUID

// TODO: hvordan henter vi migreringInfoen for brev?
class MigreringKlient(config: Config, httpClient: HttpClient) {
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("migrering.client.id")
    private val resourceUrl = config.getString("migrering.resource.url")

    suspend fun hentMigreringRequest(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): MigreringRequest {
        try {
            return downstreamResourceClient.get(Resource(clientId, "$resourceUrl/todo"), brukerTokenInfo)
                .mapBoth(
                    success = { resource -> deserialize(resource.response.toString()) },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            throw MigreringKlientException(
                "Henting av migreringrequest for behandlind med behandlindId=$behandlingId feilet",
                e,
            )
        }
    }
}

class MigreringKlientException(override val detail: String, override val cause: Throwable?) :
    InternfeilException(detail, cause)
