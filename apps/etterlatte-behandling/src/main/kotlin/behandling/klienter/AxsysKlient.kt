package no.nav.etterlatte.behandling.klienter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.Pingable
import no.nav.etterlatte.libs.ktor.navConsumerId
import no.nav.etterlatte.libs.ktor.ping
import no.nav.etterlatte.saksbehandler.SaksbehandlerEnhet
import org.slf4j.LoggerFactory
import java.time.Duration

interface AxsysKlient : Pingable {
    suspend fun hentEnheterForIdent(ident: String): List<SaksbehandlerEnhet>
}

class AxsysKlientImpl(
    private val client: HttpClient,
    private val url: String,
) : AxsysKlient {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val enhetCache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .build<String, List<SaksbehandlerEnhet>>()

    override suspend fun hentEnheterForIdent(ident: String): List<SaksbehandlerEnhet> {
        logger.info("Henter enheter fra Axsys for saksbehandlerident $ident.")

        val cachedEnhet = enhetCache.getIfPresent(ident)
        if (cachedEnhet != null) {
            return cachedEnhet
        }
        return try {
            val response =
                client.get("$url/api/v2/tilgang/$ident?inkluderAlleEnheter=false") {
                    navConsumerId("etterlatte-behandling")
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                }

            response
                .body<EnhetslisteResponse?>()
                ?.enheter
                ?.map { SaksbehandlerEnhet(it.enhetId, it.navn) }
                ?.also {
                    enhetCache.put(ident, it)
                } ?: emptyList()
        } catch (cause: Throwable) {
            val feilmelding = "Klarte ikke å hente enheter for ident $ident fra axsys."
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
        get() = "Axsysklient"
    override val beskrivelse: String
        get() = "Sjekker om en person er skjermet"
    override val endpoint: String
        get() = this.url
}

class HentEnhetException(
    override val detail: String,
    override val cause: Throwable?,
) : InternfeilException(detail, cause)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Enheter(
    // Enhetsnummer
    val enhetId: Enhetsnummer,
    // EYB EYO
    val temaer: ArrayList<String>?,
    val navn: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EnhetslisteResponse(
    val enheter: List<Enheter>,
)
