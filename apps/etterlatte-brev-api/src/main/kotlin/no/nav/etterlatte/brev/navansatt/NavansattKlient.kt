package no.nav.etterlatte.brev.navansatt

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.isSuccess
import no.nav.etterlatte.brev.adresse.AdresseException
import no.nav.etterlatte.libs.common.logging.NAV_CALL_ID
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import org.slf4j.LoggerFactory
import java.time.Duration

class NavansattKlient(
    private val client: HttpClient,
    private val url: String
) {
    private val logger = LoggerFactory.getLogger(NavansattKlient::class.java)

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(15))
        .build<String, SaksbehandlerInfo>()

    suspend fun hentSaksbehandlerInfo(ident: String): SaksbehandlerInfo = try {
        val saksbehandlerCache = cache.getIfPresent(ident)

        if (saksbehandlerCache != null) {
            logger.info("Fant cachet saksbehandler med ident $ident")
            saksbehandlerCache
        } else {
            logger.info("Henter info om saksbehandler med ident $ident")

            val response = client.get("$url/navansatt/$ident") {
                header(X_CORRELATION_ID, getXCorrelationId())
                header(NAV_CALL_ID, getXCorrelationId())
            }

            if (response.status.isSuccess()) {
                response.body<SaksbehandlerInfo>()
                    .also { cache.put(ident, it) }
            } else {
                throw ResponseException(response, "Ukjent feil fra navansatt api")
            }
        }
    } catch (exception: Exception) {
        throw AdresseException("Feil i kall mot navansatt med ident: $ident", exception)
    }
}