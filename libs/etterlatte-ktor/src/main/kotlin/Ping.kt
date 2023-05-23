package no.nav.etterlatte.libs.ktor

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import org.slf4j.Logger

suspend fun HttpClient.ping(
    url: String,
    logger: Logger,
    serviceName: String,
    beskrivelse: String,
    konsument: String
): PingResult {
    return try {
        this.get(url) {
            accept(ContentType.Application.Json)
            header(X_CORRELATION_ID, getXCorrelationId())
            header("Nav_Call_Id", getXCorrelationId())
            header("Nav-Consumer-Id", konsument)
        }
        logger.info("$serviceName svarer OK")
        PingResult.up(serviceName, url, beskrivelse)
    } catch (e: Exception) {
        PingResult.down(serviceName, url, e.message, beskrivelse).also {
            logger.warn("$serviceName svarer IKKE ok. ${it.toStringServiceDown()}")
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun asyncPing(pingFunc: suspend () -> PingResult) {
    GlobalScope.launch(newSingleThreadContext("pingThread")) {
        pingFunc()
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