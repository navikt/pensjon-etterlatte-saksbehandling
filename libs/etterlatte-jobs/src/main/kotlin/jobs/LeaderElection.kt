package no.nav.etterlatte.libs.jobs

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.appIsInGCP
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import org.slf4j.LoggerFactory
import java.net.ConnectException
import java.net.InetAddress

class LeaderElectionDownException(
    message: String,
    override val cause: Throwable,
) : InternfeilException(detail = message)

open class LeaderElection(
    private val electorPath: String?,
    private val httpClient: HttpClient = HttpClient(),
) {
    private val logger = LoggerFactory.getLogger(LeaderElection::class.java)
    private val objectMapper = jacksonObjectMapper()

    open fun hostName(): String? = InetAddress.getLocalHost().hostName

    open fun isLeader(): Boolean {
        if (electorPath != null) {
            val leader =
                try {
                    runBlocking {
                        httpClient
                            .get("http://$electorPath/")
                            .bodyAsText()
                            .let(objectMapper::readTree)
                            .get("name")
                            .asText()
                    }
                } catch (e: ConnectException) {
                    throw LeaderElectionDownException("Kan ikke nå leaderelection", e)
                }
            val isLeader = leader == hostName()
            logger.info("Current pod: ${hostName()?.sanitize()}. Leader: ${leader.sanitize()}. Current pod is leader: $isLeader")
            return isLeader
        } else {
            if (appIsInGCP()) {
                throw RuntimeException("Kan ikke bruke leaderElection i GCP uten å sette leaderElection: true i yaml fil")
            } else {
                return true // local machine has no knowledge of leader concept(non-k8s run)
            }
        }
    }
}

private val sanitizeRegex = Regex("[A-Za-z0-9.-]+$")

fun String.sanitize() = this.takeIf { it.matches(sanitizeRegex) } ?: "Ugyldig verdi"
