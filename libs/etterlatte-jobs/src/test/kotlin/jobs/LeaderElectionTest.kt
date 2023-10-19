package no.nav.etterlatte.libs.jobs

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.serialization.jackson.JacksonConverter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class LeaderElectionTest {
    companion object {
        private val localHostName = "localhost"

        fun httpClient(response: String) =
            HttpClient(MockEngine) {
                expectSuccess = true
                engine {
                    addHandler { request ->
                        when (request.url.fullPath) {
                            "/" -> respond(response)
                            else -> respond("error", HttpStatusCode.BadGateway)
                        }
                    }
                }
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(jacksonObjectMapper()))
                }
            }
    }

    @Test
    fun `sier ja når leader elector-pod svarer at vårt hostname er leader`() {
        val response = """{"name":"$localHostName"}"""
        val httpClient =
            httpClient(
                response,
            )
        val leaderElection = LeaderElectionLocalhost(localHostName, httpClient)
        assertTrue(leaderElection.isLeader())
    }

    @Test
    fun `sier nei når leader elector-pod svarer at noen andre er leader`() {
        val response = """{"name":"NOT_LEADER"}""".trimIndent()
        val httpClient = httpClient(response)
        val leaderElection = LeaderElectionLocalhost(localHostName, httpClient)
        assertFalse(leaderElection.isLeader())
    }

    @Test
    fun testsanitize() {
        Assertions.assertEquals("etterlatte-behandling-696cf4f4b7-tpvhm", "etterlatte-behandling-696cf4f4b7-tpvhm".sanitize())
        Assertions.assertEquals("127.0.0.1", "127.0.0.1".sanitize())
        Assertions.assertEquals("Ugyldig verdi", "Guest'%0AUser:'Admin".sanitize())
        Assertions.assertEquals("Ugyldig verdi", "*!\"#!\"$\"!$!\"#\"!$:::::::".sanitize())
    }
}

class LeaderElectionLocalhost(electorPath: String, httpClient: HttpClient) : LeaderElection(electorPath, httpClient) {
    override fun me() = "localhost"
}
