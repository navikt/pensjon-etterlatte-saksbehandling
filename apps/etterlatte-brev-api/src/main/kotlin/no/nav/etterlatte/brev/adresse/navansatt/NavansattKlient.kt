package no.nav.etterlatte.brev.adresse.navansatt

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.retry
import org.slf4j.LoggerFactory
import java.time.Duration

class NavansattKlient(
    private val client: HttpClient,
    private val url: String,
) {
    private val logger = LoggerFactory.getLogger(NavansattKlient::class.java)

    private val cache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .build<String, SaksbehandlerInfo>()

    suspend fun hentSaksbehandlerInfo(ident: String): SaksbehandlerInfo =
        retry {
            hentSaksbehandler(ident)
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> {
                    logger.warn("Feil ved uthenting av saksbehandlerInfo", it.samlaExceptions())
                    throw it.samlaExceptions()
                }
            }
        }

    private suspend fun hentSaksbehandler(ident: String): SaksbehandlerInfo =
        try {
            val saksbehandlerCache = cache.getIfPresent(ident)

            if (saksbehandlerCache != null) {
                logger.info("Fant cachet saksbehandler med ident $ident")
                saksbehandlerCache
            } else {
                logger.info("Henter info om saksbehandler med ident $ident")

                client
                    .get("$url/navansatt/$ident")
                    .body<SaksbehandlerInfo>()
                    .also {
                        try {
                            cache.put(ident, it)
                            logger.info("Info funnet og cachet for saksbehandler med ident $ident")
                        } catch (e: Exception) {
                            logger.warn("Feil ved caching av saksbehandler med ident $ident", e)
                        }
                    }
            }
        } catch (re: ResponseException) {
            logger.warn("Fikk ResponseException ved kall mot navansattklient", re)
            throw re
        } catch (exception: Exception) {
            logger.warn("Feil i kall mot navansatt")
            throw RuntimeException("Feil i kall mot navansatt med ident: $ident", exception)
        }
}
