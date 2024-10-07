package no.nav.etterlatte.brev.adresse.navansatt

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import no.nav.etterlatte.libs.common.retryOgPakkUt
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

    suspend fun hentSaksbehandlerInfo(ident: String): SaksbehandlerInfo = retryOgPakkUt { hentSaksbehandler(ident) }

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
                    .also { cache.put(ident, it) }
            }
        } catch (re: ResponseException) {
            logger.warn("Fikk ResponseException ved kall mot navansattklient", re)
            throw re
        } catch (exception: Exception) {
            throw RuntimeException("Feil i kall mot navansatt med ident: $ident", exception)
        }
}
