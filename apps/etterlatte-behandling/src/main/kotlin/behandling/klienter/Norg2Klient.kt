package no.nav.etterlatte.behandling.klienter

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingRequest
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import org.slf4j.LoggerFactory
import java.time.Duration

interface Norg2Klient {
    fun hentEnheterForOmraade(
        tema: String,
        omraade: String
    ): List<ArbeidsFordelingEnhet>
}

class Norg2KlientImpl(private val client: HttpClient, private val url: String) : Norg2Klient {
    private val logger = LoggerFactory.getLogger(Norg2KlientImpl::class.java)

    private val cache = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(15))
        .build<ArbeidsFordelingRequest, List<ArbeidsFordelingEnhet>>()

    override fun hentEnheterForOmraade(tema: String, omraade: String): List<ArbeidsFordelingEnhet> =
        hent(ArbeidsFordelingRequest(tema, omraade))

    private fun hent(request: ArbeidsFordelingRequest): List<ArbeidsFordelingEnhet> = try {
        val cachedInfo = cache.getIfPresent(request)

        if (cachedInfo != null) {
            logger.info("Fant cachet enheter for tema og omraade $request")
            cachedInfo
        } else {
            logger.info("Henter enheter for tema og omraade $request")

            runBlocking {
                val response = client.get("$url/api/v1/arbeidsfordeling/enheter/bestmatch") {
                    header("x_correlation_id", getXCorrelationId())
                    header("Nav_Call_Id", getXCorrelationId())
                }

                if (response.status.isSuccess()) {
                    response.body<List<ArbeidsFordelingEnhet>>().also { cache.put(request, it) }
                } else {
                    throw ResponseException(response, "Ukjent feil fra norg2 api")
                }
            }
        }
    } catch (exception: Exception) {
        throw RuntimeException("Feil i kall mot norg2 med tema og omraade: $request", exception)
    }
}