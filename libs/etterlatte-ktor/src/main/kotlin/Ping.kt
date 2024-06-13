package no.nav.etterlatte.libs.ktor

import com.fasterxml.jackson.annotation.JsonValue
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
        PingResultUp(serviceName, endpoint = pingUrl, description = beskrivelse)
    } catch (e: Exception) {
        PingResultDown(serviceName, endpoint = pingUrl, errorMessage = e.message, description = beskrivelse).also {
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
    abstract val result: ServiceStatus
    abstract val endpoint: String
    abstract val description: String
}

class PingResultUp(
    override val serviceName: String,
    override val result: ServiceStatus = ServiceStatus.UP,
    override val endpoint: String,
    override val description: String,
) : PingResult()

class PingResultDown(
    override val serviceName: String,
    override val result: ServiceStatus = ServiceStatus.DOWN,
    override val endpoint: String,
    override val description: String,
    val errorMessage: String?,
) : PingResult() {
    fun toStringServiceDown(): String =
        "Servicename: $serviceName endpoint: $endpoint beskrivelse $description errorMessage: $errorMessage "
}

enum class ServiceStatus(
    @JsonValue private val code: Int,
) {
    DOWN(1), // red
    UP(0), // green
    ;

    /**
     * 0 = OK, 1 = ERROR, ref. https://confluence.adeo.no/display/AURA/Selftest
     */
    fun code(): Int = code

    fun codeToColour(): String =
        when (code) {
            1 -> "red"
            2 -> "green"
            else -> throw IllegalStateException("Ugyldig kode: $code for status til tjeneste")
        }
}
