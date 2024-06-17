package no.nav.etterlatte.behandling.klienter

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.Pingable
import no.nav.etterlatte.libs.ktor.ping
import org.slf4j.LoggerFactory

data class SaksbehandlerInfo(
    val ident: String,
    val navn: String,
)

interface NavAnsattKlient : Pingable {
    suspend fun hentSaksbehanderNavn(ident: String): SaksbehandlerInfo?
}

class NavAnsattKlientImpl(
    private val client: HttpClient,
    private val url: String,
) : NavAnsattKlient {
    private val logger = LoggerFactory.getLogger(NavAnsattKlientImpl::class.java)

    private val navneCache =
        Caffeine
            .newBuilder()
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

    override val serviceName: String
        get() = "Navansatt"
    override val beskrivelse: String
        get() = "Henter enheter for saksbehandlerident"
    override val endpoint: String
        get() = this.url

    override suspend fun ping(konsument: String?): PingResult =
        client.ping(
            pingUrl = url.plus("/ping"),
            logger = logger,
            serviceName = serviceName,
            beskrivelse = beskrivelse,
            konsument = konsument,
        )
}
