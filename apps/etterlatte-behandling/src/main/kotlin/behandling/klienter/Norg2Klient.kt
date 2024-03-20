package no.nav.etterlatte.behandling.klienter

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingRequest
import no.nav.etterlatte.behandling.domain.Navkontor
import org.slf4j.LoggerFactory
import java.time.Duration

interface Norg2Klient {
    fun hentArbeidsfordelingForOmraadeOgTema(request: ArbeidsFordelingRequest): List<ArbeidsFordelingEnhet>

    suspend fun hentNavkontorForOmraade(omraade: String): Navkontor
}

class Norg2KlientImpl(private val client: HttpClient, private val url: String) : Norg2Klient {
    private val logger = LoggerFactory.getLogger(Norg2KlientImpl::class.java)

    private val cacheNavkontor =
        Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(15))
            .build<String, Navkontor>()

    override suspend fun hentNavkontorForOmraade(omraade: String): Navkontor {
        val maybeNavkontor = cacheNavkontor.getIfPresent(omraade)

        return if (maybeNavkontor != null) {
            maybeNavkontor
        } else {
            val response =
                client.get("$url/enhet/navkontor/$omraade") {
                    contentType(ContentType.Application.Json)
                }

            if (response.status.isSuccess()) {
                response.body<Navkontor>().also { cacheNavkontor.put(omraade, it) }
            } else {
                throw ResponseException(response, "Ukjent feil fra norg2 api")
            }
        }
    }

    override fun hentArbeidsfordelingForOmraadeOgTema(request: ArbeidsFordelingRequest): List<ArbeidsFordelingEnhet> =
        try {
            logger.info("Henter enheter for tema og omraade $request")
            runBlocking {
                val response =
                    client.post("$url/arbeidsfordeling/enheter/bestmatch") {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }

                if (response.status.isSuccess()) {
                    response.body<List<ArbeidsFordelingEnhet>>()
                } else {
                    throw ResponseException(response, "Ukjent feil fra norg2 api")
                }
            }
        } catch (exception: Exception) {
            throw RuntimeException("Feil i kall mot norg2 med tema og omraade: $request", exception)
        }
}
