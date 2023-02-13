package no.nav.etterlatte.brev.navansatt

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import no.nav.etterlatte.brev.adresse.AdresseException
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

            client.get("$url/navansatt/$ident") {
                header("x_correlation_id", getXCorrelationId())
                header("Nav_Call_Id", getXCorrelationId())
            }.body()
        }
    } catch (exception: Exception) {
        throw AdresseException("Feil i kall mot navansatt med ident: $ident", exception)
    }
}