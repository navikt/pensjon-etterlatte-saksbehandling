package no.nav.etterlatte.behandling.klienter

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.isSuccess
import no.nav.etterlatte.behandling.domain.SaksbehandlerEnhet
import no.nav.etterlatte.behandling.domain.SaksbehandlerTema
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import org.slf4j.LoggerFactory
import java.time.Duration

private enum class InfoType(val urlSuffix: String) {
    ENHET("/enheter"),
    TEMA("/fagomrader")
}

class NavAnsattKlient(
    private val client: HttpClient,
    private val url: String
) {
    private val logger = LoggerFactory.getLogger(NavAnsattKlient::class.java)

    private val enhetCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(15))
        .build<String, List<SaksbehandlerEnhet>>()

    private val temaCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(15))
        .build<String, List<SaksbehandlerTema>>()

    suspend fun hentSaksbehandlerEnhet(ident: String): List<SaksbehandlerEnhet> = hentSaksbehandler(
        ident,
        InfoType.ENHET,
        enhetCache
    )

    suspend fun hentSaksbehandlerTema(ident: String): List<SaksbehandlerTema> = hentSaksbehandler(
        ident,
        InfoType.TEMA,
        temaCache
    )

    private suspend inline fun <reified T : List<Any>> hentSaksbehandler(
        ident: String,
        infoType: InfoType,
        cache: Cache<String, T>
    ): T =
        try {
            val cachedInfo = cache.getIfPresent(ident)

            if (cachedInfo != null) {
                logger.info("Fant cachet saksbehandler info med ident $ident")
                cachedInfo
            } else {
                logger.info("Henter enhet for saksbehandler med ident $ident")

                val response = client.get("$url/navansatt/$ident/${infoType.urlSuffix}") {
                    header("x_correlation_id", getXCorrelationId())
                    header("Nav_Call_Id", getXCorrelationId())
                }
                if (response.status.isSuccess()) {
                    response.body<T>()
                        .also { cache.put(ident, it) }
                } else {
                    throw ResponseException(response, "Ukjent feil fra navansatt api")
                }
            }
        } catch (exception: Exception) {
            throw RuntimeException("Feil i kall mot navansatt med ident: $ident", exception)
        }
}