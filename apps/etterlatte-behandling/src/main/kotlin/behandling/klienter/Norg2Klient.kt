package no.nav.etterlatte.behandling.klienter

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingRequest
import no.nav.etterlatte.behandling.domain.Navkontor
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import org.slf4j.LoggerFactory
import java.time.Duration

interface Norg2Klient {
    fun hentArbeidsfordelingForOmraadeOgTema(request: ArbeidsFordelingRequest): List<ArbeidsFordelingEnhet>

    suspend fun hentNavkontorForOmraade(omraade: String): Navkontor
}

class Norg2KlientFeil(
    feilmelding: String,
    override val cause: Throwable,
) : InternfeilException("$feilmelding ${cause.message}", cause)

class Norg2KlientImpl(
    private val client: HttpClient,
    private val url: String,
) : Norg2Klient {
    private val logger = LoggerFactory.getLogger(Norg2KlientImpl::class.java)

    private val cacheNavkontor =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build<String, Navkontor>()

    override suspend fun hentNavkontorForOmraade(omraade: String): Navkontor =
        cacheNavkontor.getIfPresent(omraade) ?: hentNavkontor(omraade)

    private suspend fun hentNavkontor(omraade: String): Navkontor {
        try {
            val response =
                client.get("$url/enhet/navkontor/$omraade") {
                    contentType(ContentType.Application.Json)
                }
            return if (response.status.isSuccess()) {
                response.body<Navkontor>().also { cacheNavkontor.put(omraade, it) }
            } else if (omraade == "0301" && response.status == HttpStatusCode.NotFound) {
                Navkontor("Ingen", Enhetsnummer.ingenTilknytning)
            } else {
                throw ResponseException(response, "Ukjent feil fra norg2 api")
            }
        } catch (e: Exception) {
            throw Norg2KlientFeil("Feil i kall mot norg2 for område $omraade", e)
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
            throw Norg2KlientFeil("Feil i kall mot norg2 med tema og omraade: $request", exception)
        }
}
