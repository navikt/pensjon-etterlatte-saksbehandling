package no.nav.etterlatte.brev.trygdetid

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.*

class TrygdetidKlientException(override val message: String, override val cause: Throwable) :
    Exception(message, cause)

class TrygdetidKlient(config: Config, httpClient: HttpClient) {
    private val logger = LoggerFactory.getLogger(TrygdetidKlient::class.java)
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("trygdetid.client.id")
    private val resourceUrl = config.getString("trygdetid.resource.url")

    suspend fun hentTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo
    ): TrygdetidDto? {
        try {
            logger.info("Henter trygdetid med behandlingid $behandlingId")
            return downstreamResourceClient.get(
                Resource(clientId, "$resourceUrl/api/trygdetid/$behandlingId"),
                brukerTokenInfo
            ).mapBoth(
                success = { resource -> resource.response.let { deserialize(it.toString()) } },
                failure = { throwableErrorMessage -> throw throwableErrorMessage }
            )
        } catch (e: Exception) {
            throw TrygdetidKlientException("Henting av trygdetid for sak med behandlingsid=$behandlingId feilet", e)
        }
    }
}