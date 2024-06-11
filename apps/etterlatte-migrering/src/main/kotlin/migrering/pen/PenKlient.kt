package no.nav.etterlatte.migrering.pen

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import org.slf4j.LoggerFactory

class PenKlient(
    config: Config,
    pen: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, pen)

    private val clientId = config.getString("pen.client.id")
    private val resourceUrl = config.getString("pen.client.url")

    suspend fun opphoerSak(pesysId: PesysId) {
        logger.info("Opphører sak $pesysId i PEN")
        downstreamResourceClient
            .post(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/barnepensjon-migrering/opphoer?sakId=${pesysId.id}",
                    ),
                brukerTokenInfo = Systembruker.migrering,
                postBody = {},
            ).mapBoth(
                success = { logger.info("Opphørte sak $pesysId mot PEN") },
                failure = { errorResponse -> throw errorResponse },
            )
    }
}
