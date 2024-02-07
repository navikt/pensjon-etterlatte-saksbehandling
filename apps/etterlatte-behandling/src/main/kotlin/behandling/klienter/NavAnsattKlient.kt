package no.nav.etterlatte.behandling.klienter

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import no.nav.etterlatte.behandling.domain.SaksbehandlerEnhet
import no.nav.etterlatte.behandling.domain.SaksbehandlerTema
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.Pingable
import no.nav.etterlatte.libs.ktor.ping
import org.slf4j.LoggerFactory
import java.time.Duration

private enum class InfoType(val urlSuffix: String) {
    ENHET("enheter"),
    TEMA("fagomrader"),
}

data class SaksbehandlerInfo(
    val ident: String,
    val navn: String,
)

interface NavAnsattKlient {
    suspend fun hentEnhetForSaksbehandler(ident: String): List<SaksbehandlerEnhet>

    suspend fun hentTemaForSaksbehandler(ident: String): List<SaksbehandlerTema>

    suspend fun hentSaksbehanderNavn(ident: String): SaksbehandlerInfo?
}

class NavAnsattKlientImpl(
    private val client: HttpClient,
    private val url: String,
) : NavAnsattKlient, Pingable {
    private val logger = LoggerFactory.getLogger(NavAnsattKlientImpl::class.java)
    private val enhetCache =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .build<String, List<SaksbehandlerEnhet>>()

    private val temaCache =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .build<String, List<SaksbehandlerTema>>()

    private val navneCache =
        Caffeine.newBuilder()
            .build<String, SaksbehandlerInfo>()

    override suspend fun hentSaksbehanderNavn(ident: String): SaksbehandlerInfo? =
        try {
            val saksbehandlerCache = navneCache.getIfPresent(ident)

            if (saksbehandlerCache != null) {
                logger.info("Fant cachet saksbehandlernavn med ident $ident")
                saksbehandlerCache
            } else {
                logger.info("Henter info om saksbehandlernavn med ident $ident")

                retryOgPakkUt<SaksbehandlerInfo?> {
                    client.get("$url/navansatt/$ident").body()
                }.also { navneCache.put(ident, it) }
            }
        } catch (exception: Exception) {
            throw RuntimeException("Feil i kall mot navansatt navn med ident: $ident", exception)
        }

    override suspend fun hentEnhetForSaksbehandler(ident: String): List<SaksbehandlerEnhet> =
        hentSaksbehandler(
            ident,
            InfoType.ENHET,
            enhetCache,
        )

    override suspend fun hentTemaForSaksbehandler(ident: String): List<SaksbehandlerTema> =
        hentSaksbehandler(
            ident,
            InfoType.TEMA,
            temaCache,
        )

    private suspend inline fun <reified T : List<Any>> hentSaksbehandler(
        ident: String,
        infoType: InfoType,
        cache: Cache<String, T>,
    ): T =
        try {
            val cachedInfo = cache.getIfPresent(ident)

            if (cachedInfo != null) {
                logger.info("Fant cachet saksbehandler info med ident $ident")
                cachedInfo
            } else {
                logger.info("Henter enhet for saksbehandler med ident $ident")

                retryOgPakkUt<T> {
                    client.get("$url/navansatt/$ident/${infoType.urlSuffix}").body()
                }.also { cache.put(ident, it) }
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
            konsument = "etterlatte-behandling",
        )
    }
}
