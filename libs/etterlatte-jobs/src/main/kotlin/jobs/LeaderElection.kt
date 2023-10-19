package no.nav.etterlatte.libs.jobs

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.net.InetAddress

open class LeaderElection(
    private val electorPath: String,
    private val httpClient: HttpClient = HttpClient(),
) {
    private val objectMapper = jacksonObjectMapper()

    open fun me(): String? = InetAddress.getLocalHost().hostName

    open fun isLeader(): Boolean {
        val leader =
            runBlocking {
                httpClient.get("http://$electorPath/").bodyAsText().let(objectMapper::readTree).get("name").asText()
            }
        val amLeader = leader == me()
        logger.info("Current pod: ${me()?.sanitize()}. Leader: ${leader.sanitize()}. Current pod is leader: $amLeader")
        return amLeader
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LeaderElection::class.java)
    }
}

private val sanitizeRegex = Regex("[A-Za-z0-9.-]+$")

fun String.sanitize() = this.takeIf { it.matches(sanitizeRegex) } ?: "Ugyldig verdi"
