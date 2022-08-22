package no.nav.etterlatte.enhetsregister

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.slf4j.LoggerFactory

class EnhetsregKlient(
    private val host: String,
    private val httpClient: HttpClient
) {
    private val logger = LoggerFactory.getLogger(EnhetsregKlient::class.java)

    suspend fun hentEnheter(navn: String): List<Enhet> {
        return try {
            httpClient
                .get("$host/enhetsregisteret/api/enheter?navn=$navn&konkurs=false&underAvvikling=false")
                .body<ResponseWrapper>()
                ._embedded
                ?.enheter
                ?: emptyList()
        } catch (re: ResponseException) {
            val feilmelding = re.response.body<Feilmelding>()

            logger.warn("Feilmelding fra brreg: $feilmelding")

            throw re
        }
    }

    suspend fun hentEnhet(orgnr: String): Enhet? {
        return try {
            httpClient.get("$host/enhetsregisteret/api/enheter/$orgnr").body()
        } catch (re: ResponseException) {
            if (re.response.status == HttpStatusCode.NotFound) {
                return null
            }

            val feilmelding = re.response.body<Feilmelding>()

            logger.warn("Feilmelding fra brreg: $feilmelding")

            throw re
        }
    }
}

class ResponseWrapper(val _embedded: Embedded?) {
    class Embedded(val enheter: List<Enhet>)
}