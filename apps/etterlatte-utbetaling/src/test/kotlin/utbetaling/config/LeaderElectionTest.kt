package no.nav.etterlatte.utbetaling.config

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class LeaderElectionTest {

    companion object {

        private val localHostName = "localhost"

        fun httpClient(response: String) = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.fullPath) {
                        "/" -> respond(response)
                        else -> respond("error", HttpStatusCode.BadGateway)
                    }
                }
            }
            install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
        }

    }

    @Test
    fun `sier ja når leader elector-pod svarer at vårt hostname er leader`() {

        val response = """{"name":"$localHostName"}"""
        val httpClient = httpClient(
            response
        )
        val leaderElection = LeaderElection(localHostName, httpClient, "localhost")
        assertTrue(leaderElection.isLeader())
    }

    @Test
    fun `sier nei når leader elector-pod svarer at noen andre er leader`() {
        val response = """{"name":"NOT_LEADER"}""".trimIndent()
        val httpClient = httpClient(response)
        val leaderElection = LeaderElection(localHostName, httpClient, "localhost")
        assertFalse(leaderElection.isLeader())
    }

}

