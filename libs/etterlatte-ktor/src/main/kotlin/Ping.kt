package no.nav.etterlatte.libs.ktor

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import no.nav.etterlatte.libs.common.appName
import org.slf4j.Logger

suspend fun HttpClient.ping(
    pingUrl: String,
    logger: Logger,
    serviceName: String,
    beskrivelse: String,
    konsument: String? = null,
): PingResult {
    val konsumentEndelig = konsument ?: appName() ?: throw RuntimeException("Må ha konsument")
    return try {
        this.get(pingUrl) {
            accept(ContentType.Application.Json)
            navConsumerId(konsumentEndelig)
        }
        logger.info("$serviceName svarer OK")
        PingResultUp(serviceName, endpoint = pingUrl, message = beskrivelse)
    } catch (e: Exception) {
        PingResultDown(serviceName, endpoint = pingUrl, errorMessage = e.message, message = beskrivelse).also {
            logger.warn("$serviceName svarer IKKE ok. ${it.toStringServiceDown()}")
        }
    }
}

interface Pingable {
    val serviceName: String
    val beskrivelse: String
    val endpoint: String

    suspend fun ping(konsument: String? = null): PingResult

    @OptIn(DelicateCoroutinesApi::class)
    fun asyncPing() {
        GlobalScope.launch(newSingleThreadContext("pingThread")) {
            ping()
        }
    }
}

sealed class PingResult {
    abstract val serviceName: String
    abstract val status: ServiceStatus
    abstract val endpoint: String
    abstract val message: String
}

class PingResultUp(
    override val serviceName: String,
    override val status: ServiceStatus = ServiceStatus.UP,
    override val endpoint: String,
    override val message: String,
) : PingResult()

class PingResultDown(
    override val serviceName: String,
    override val status: ServiceStatus = ServiceStatus.DOWN,
    override val endpoint: String,
    override val message: String,
    val errorMessage: String?,
) : PingResult() {
    fun toStringServiceDown(): String {
        return "Servicename: $serviceName endpoint: $endpoint beskrivelse $message errorMessage: $errorMessage "
    }
}

enum class ServiceStatus(private val code: Int, private val color: String) {
    DOWN(1, "red"),
    UP(0, "green"),
    ;

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
