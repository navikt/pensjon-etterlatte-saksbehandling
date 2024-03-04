package no.nav.etterlatte.behandling.klienter

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeExactly
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.saksbehandler.SaksbehandlerEnhet
import org.junit.jupiter.api.Test

internal class AxsysKlientTest {
    @Test
    fun `hent enheter skal returnere en liste av enheter`() {
        val testNavIdent = "ident1"

        val saksbehandlerEnheter =
            listOf(
                SaksbehandlerEnhet("id1", "navn1"),
                SaksbehandlerEnhet("id2", "navn2"),
                SaksbehandlerEnhet("id3", "navn3"),
            )

        val klient =
            mockHttpClient(
                EnhetslisteResponse(saksbehandlerEnheter.map { Enheter(it.enhetsNummer, null, it.navn) }),
                testNavIdent,
            )

        val axsys: AxsysKlient = AxsysKlientImpl(klient, "")

        runBlocking {
            val resultat = axsys.hentEnheterForIdent(testNavIdent)

            resultat.size shouldBeExactly 3

            resultat shouldContainExactly saksbehandlerEnheter
        }
    }

    private val defaultHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

    private fun mockHttpClient(
        respons: EnhetslisteResponse,
        ident: String,
    ): HttpClient {
        val httpClient =
            HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        when (request.url.fullPath) {
                            "/api/v2/tilgang/$ident?inkluderAlleEnheter=false" ->
                                respond(
                                    respons.toJson(),
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
