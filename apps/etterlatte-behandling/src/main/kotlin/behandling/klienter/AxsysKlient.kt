package no.nav.etterlatte.behandling.klienter

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.etterlatte.behandling.domain.SaksbehandlerEnhet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

interface AxsysKlient {
    suspend fun hentEnheterForIdent(ident: String): List<SaksbehandlerEnhet>
}

class AxsysKlientImpl(private val client: HttpClient, private val url: String) : AxsysKlient {
    private val logger: Logger = LoggerFactory.getLogger(AxsysKlientImpl::class.java)

    private val enhetCache =
        Caffeine.newBuilder()
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
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                }

            if (response.status.isSuccess()) {
                response.body<EnhetslisteResponse?>()?.enheter
                    ?.filter { it.enhetId == null || it.navn == null }
                    ?.map { SaksbehandlerEnhet(it.enhetId!!, it.navn!!) } ?: emptyList()
            } else {
                throw ClientRequestException(response, response.toString())
            }
        } catch (cause: Throwable) {
            logger.warn("Klarte ikke å hente enheter for ident $ident fra axsys.", cause)
            return emptyList()
        }
    }
}

class Enheter {
    var enhetId: String? = null // Enhetsnummer
    var temaer: ArrayList<String>? = null // EYB EYO
    var navn: String? = null
}

class EnhetslisteResponse {
    var enheter: List<Enheter>? = null
}
