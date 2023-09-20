package no.nav.etterlatte.grunnlagsendring.klienter

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GrunnlagKlientOboTest {
    private val defaultHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

    private val config: Config = mockk(relaxed = true)

    @Test
    fun `skal hente grunnlag`() {
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val klient = GrunnlagKlientImpl(config, mockHttpClient(grunnlag))
        val hentetGrunnlag =
            runBlocking {
                klient.hentGrunnlag(1)
            }
        assertEquals(grunnlag.soeker, hentetGrunnlag?.soeker)
    }

    private fun mockHttpClient(grunnlagResponse: Grunnlag): HttpClient {
        val httpClient =
            HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        when (request.url.fullPath) {
                            "/grunnlag/1" ->
                                respond(
                                    grunnlagResponse.toJson(),
                                    HttpStatusCode.OK,
                                    defaultHeaders,
                                )
                            else -> error("Unhandled ${request.url.fullPath}")
                        }
                    }
                }
                expectSuccess = true
                install(ContentNegotiation) {
                    jackson {
                        registerModule(JavaTimeModule())
                    }
                }
            }

        return httpClient
    }
}
