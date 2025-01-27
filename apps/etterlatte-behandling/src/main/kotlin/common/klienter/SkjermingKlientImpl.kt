package no.nav.etterlatte.common.klienter

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.PingResultDown
import no.nav.etterlatte.libs.ktor.PingResultUp
import no.nav.etterlatte.libs.ktor.Pingable
import org.slf4j.LoggerFactory

interface SkjermingKlient : Pingable {
    suspend fun personErSkjermet(fnr: String): Boolean
}

class SkjermingKlientImpl(
    private val httpClient: HttpClient,
    private val url: String,
) : SkjermingKlient {
    val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun personErSkjermet(fnr: String): Boolean =
        httpClient
            .post("$url/skjermet") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(SkjermetDataRequestDTO(personident = fnr))
            }.body()

    override suspend fun ping(konsument: String?): PingResult {
        try {
            httpClient
                .post("$url/skjermet") {
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(SkjermetDataRequestDTO(personident = "dummy")) // Det er meningen å sende inn "dummy"
                }.body<Boolean>()
        } catch (e: Exception) {
            return PingResultDown(serviceName, endpoint = endpoint, errorMessage = e.message, description = beskrivelse)
                .also {
                    logger.warn("$serviceName svarer IKKE ok. ${it.toStringServiceDown()}")
                }
        }
        logger.info("$serviceName svarer OK")
        return PingResultUp(serviceName, endpoint = endpoint, description = beskrivelse)
    }

    override val serviceName: String
        get() = "SkjermingKlient"
    override val beskrivelse: String
        get() = "Sjekker om en person er skjermet"
    override val endpoint: String
        get() = this.url
}

data class SkjermetDataRequestDTO(
    val personident: String,
)
