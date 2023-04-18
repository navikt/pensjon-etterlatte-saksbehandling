package no.nav.etterlatte.skjerming

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import org.slf4j.LoggerFactory

class SkjermingKlient(
    private val httpClient: HttpClient,
    private val url: String
) : Pingable {
    val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun personErSkjermet(fnr: String): Boolean {
        return httpClient.post("$url/skjermet") {
            accept(ContentType.Application.Json)
            header(X_CORRELATION_ID, getXCorrelationId())
            header("Nav_Call_Id", getXCorrelationId())
            contentType(ContentType.Application.Json)
            setBody(SkjermetDataRequestDTO(personident = fnr))
        }.body()
    }

    override suspend fun ping(): PingResult {
        try {
            val skjermetFalse: Boolean = httpClient.post("$url/skjermet") {
                accept(ContentType.Application.Json)
                header(X_CORRELATION_ID, getXCorrelationId())
                header("Nav_Call_Id", getXCorrelationId())
                contentType(ContentType.Application.Json)
                setBody(SkjermetDataRequestDTO(personident = "dummy")) // Det er meningen å sende inn "dummy"
            }.body()
        } catch (e: Exception) {
            return PingResult.down(serviceName, endpoint, e.message, beskrivelse).also {
                logger.warn("Skjermingstjeneste svarer IKKE ok. ${it.toStringServiceDown()}")
            }
        }
        logger.info("Skjermingstjeneste svarer OK")
        return PingResult.up(serviceName, endpoint, beskrivelse)
    }

    override val serviceName: String
        get() = "SkjermingKlient"
    override val beskrivelse: String
        get() = "Sjekker om en person er skjermet"
    override val endpoint: String
        get() = this.url
}

data class SkjermetDataRequestDTO(
    val personident: String
)

interface Pingable {
    val serviceName: String
    val beskrivelse: String
    val endpoint: String
    suspend fun ping(): PingResult
    fun asyncPing() {
        runBlocking {
            launch(Dispatchers.IO) {
                ping()
            }
        }
    }
}

class PingResult private constructor(
    private val serviceName: String,
    private val status: ServiceStatus,
    private val endpoint: String,
    private val beskrivelse: String,
    private val errorMessage: String?
) {
    fun getStatus(): ServiceStatus {
        return status
    }

    fun toStringServiceDown(): String {
        return "Servicename: $serviceName endpoint: $endpoint beskrivelse $beskrivelse errorMessage: $errorMessage "
    }

    companion object {
        fun up(serviceName: String, endpoint: String, description: String): PingResult {
            return PingResult(serviceName, ServiceStatus.UP, endpoint, description, null)
        }

        fun down(serviceName: String, endpoint: String, errorMessage: String?, description: String): PingResult {
            return PingResult(serviceName, ServiceStatus.DOWN, endpoint, description, errorMessage)
        }
    }
}

enum class ServiceStatus(private val code: Int, private val color: String) {
    DOWN(1, "red"), UP(0, "green");

    /**
     * 0 = OK, 1 = ERROR, ref. https://confluence.adeo.no/display/AURA/Selftest
     */
    fun code(): Int {
        return code
    }

    fun color(): String {
        return color
    }
}