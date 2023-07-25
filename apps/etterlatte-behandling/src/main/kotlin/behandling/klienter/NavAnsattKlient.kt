package no.nav.etterlatte.behandling.klienter

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders.XCorrelationId
import io.ktor.http.isSuccess
import no.nav.etterlatte.behandling.domain.SaksbehandlerEnhet
import no.nav.etterlatte.behandling.domain.SaksbehandlerTema
import no.nav.etterlatte.libs.common.logging.NAV_CALL_ID
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.Pingable
import no.nav.etterlatte.libs.ktor.ping
import org.slf4j.LoggerFactory
import java.time.Duration

private enum class InfoType(val urlSuffix: String) {
    ENHET("enheter"),
    TEMA("fagomrader")
}

interface NavAnsattKlient {
    suspend fun hentSaksbehandlerEnhet(ident: String): List<SaksbehandlerEnhet>

    suspend fun hentSaksbehandlerTema(ident: String): List<SaksbehandlerTema>
}

class NavAnsattKlientImpl(
    private val client: HttpClient,
    private val url: String
) : NavAnsattKlient, Pingable {
    private val logger = LoggerFactory.getLogger(NavAnsattKlientImpl::class.java)
    private val enhetCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(15))
        .build<String, List<SaksbehandlerEnhet>>()

    private val temaCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(15))
        .build<String, List<SaksbehandlerTema>>()

    override suspend fun hentSaksbehandlerEnhet(ident: String): List<SaksbehandlerEnhet> = hentSaksbehandler(
        ident,
        InfoType.ENHET,
        enhetCache
    )

    override suspend fun hentSaksbehandlerTema(ident: String): List<SaksbehandlerTema> = hentSaksbehandler(
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
                    header(XCorrelationId, getXCorrelationId())
                    header(NAV_CALL_ID, getXCorrelationId())
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

    override val serviceName: String
        get() = "Navansatt"
    override val beskrivelse: String
        get() = "Henter enheter for saksbehandlerident"
    override val endpoint: String
        get() = this.url

    override suspend fun ping(): PingResult {
        return client.ping(
            url = url.plus("/ping"),
            logger = logger,
            serviceName = serviceName,
            beskrivelse = beskrivelse,
            konsument = "etterlatte-behandling"
        )
    }
}