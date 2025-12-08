package no.nav.etterlatte.behandling.klienter

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.Pingable
import no.nav.etterlatte.libs.ktor.navConsumerId
import no.nav.etterlatte.libs.ktor.ping
import no.nav.etterlatte.saksbehandler.SaksbehandlerEnhet
import org.slf4j.LoggerFactory
import java.time.Duration

interface EntraProxyKlient : Pingable {
    suspend fun hentEnheterForIdent(ident: String): List<SaksbehandlerEnhet>
}

class EntraProxyKlientImpl(
    private val client: HttpClient,
    private val url: String,
) : EntraProxyKlient {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val enhetCache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .build<String, List<SaksbehandlerEnhet>>()

    override suspend fun hentEnheterForIdent(ident: String): List<SaksbehandlerEnhet> {
        logger.info("Henter enheter fra EntraProxy for saksbehandlerident $ident.")

        val cachedEnhet = enhetCache.getIfPresent(ident)
        if (cachedEnhet != null) {
            return cachedEnhet
        }
        return try {
            val response =
                client.get("$url/api/enhet/ansatt/$ident") {
                    navConsumerId("etterlatte-behandling")
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                }

            response
                .body<Set<EntraEnhet>?>()
                ?.map { SaksbehandlerEnhet(Enhetsnummer(it.enhetnummer), it.navn) }
                ?.also {
                    enhetCache.put(ident, it)
                } ?: emptyList()
        } catch (cause: Throwable) {
            val feilmelding = "Klarte ikke å hente enheter for ident $ident fra EntraProxy."
            logger.error(feilmelding, cause)
            throw HentEnhetException(feilmelding, cause)
        }
    }

    override suspend fun ping(konsument: String?): PingResult =
        client.ping(
            pingUrl = url.plus("/internal/isReady"),
            logger = logger,
            serviceName = serviceName,
            beskrivelse = beskrivelse,
        )

    override val serviceName: String
        get() = "EntraProxyKlient"
    override val beskrivelse: String
        get() = "Sjekker om en person er skjermet"
    override val endpoint: String
        get() = this.url
}

data class EntraEnhet(
    val enhetnummer: String,
    val navn: String,
)
