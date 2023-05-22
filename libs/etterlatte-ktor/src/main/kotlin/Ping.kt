package no.nav.etterlatte.libs.ktor

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

interface Pingable {
    val serviceName: String
    val beskrivelse: String
    val endpoint: String
    suspend fun ping(): PingResult

    @OptIn(DelicateCoroutinesApi::class)
    fun asyncPing() {
        GlobalScope.launch(newSingleThreadContext("pingThread")) {
            ping()
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