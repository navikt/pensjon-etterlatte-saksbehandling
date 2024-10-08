package no.nav.etterlatte.brev.adresse.saksbehandler

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.Duration

class SaksbehandlerKlient(
    config: Config,
    httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    private val cache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .build<String, String>()

    internal suspend fun hentSaksbehandlerNavn(
        ident: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): String {
        val navnFraCache = cache.getIfPresent(ident)

        return if (navnFraCache != null) {
            logger.info("Fant cachet navn for saksbehandler med ident $ident")
            navnFraCache
        } else {
            logger.info("Henter navn for saksbehandler med ident $ident")

            retryOgPakkUt {
                downstreamResourceClient
                    .get(
                        resource =
                            Resource(
                                clientId = clientId,
                                url = "$resourceUrl/api/saksbehandlere/navnforident/$ident",
                            ),
                        brukerTokenInfo = brukerTokenInfo,
                    ).mapBoth(
                        success = { it.response!!.toString() },
                        failure = { throwableErrorMessage -> throw throwableErrorMessage },
                    )
            }.also { navn ->
                logger.info("Cacher navn for saksbehandler med ident $ident")
                cache.put(ident, navn)
            }
        }
    }
}
