package no.nav.etterlatte.utbetaling.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.net.InetAddress

open class LeaderElection(
    private val electorPath: String,
    private val httpClient: HttpClient = HttpClient(),
    private val me: String? = InetAddress.getLocalHost().hostName
) {
    private val objectMapper = jacksonObjectMapper()

    open fun isLeader(): Boolean {
        val leader = runBlocking {
            httpClient.get<String>("http://$electorPath/").let(objectMapper::readTree).get("name").asText()
        }
        val amLeader = leader == me
        logger.info("Current pod: $me. Leader: $leader. Current pod is leader: $amLeader")
        return amLeader
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LeaderElection::class.java)
    }
}
