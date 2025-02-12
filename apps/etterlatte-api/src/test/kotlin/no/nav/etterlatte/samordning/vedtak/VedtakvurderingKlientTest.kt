package no.nav.etterlatte.samordning.vedtak

import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VedtakvurderingKlientTest {
    private val defaultHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
    private val config = ConfigFactory.parseMap(mapOf("vedtak.url" to ""))

    @Test
    fun `hent vedtak`() {
        val vedtakId = 123L
        val client =
            createHttpClient { request ->
                when (request.url.fullPath) {
                    "/api/samordning/vedtak/$vedtakId" ->
                        respond(
                            vedtak(
                                sakstype = SakType.OMSTILLINGSSTOENAD,
                            ).toJson(),
                            HttpStatusCode.OK,
                            defaultHeaders,
                        )

                    else -> error("Unhandled ${request.url.fullPath}")
                }
            }
        val vedtaksvurderingKlient = VedtaksvurderingSamordningKlient(config, client)
        runBlocking {
            vedtaksvurderingKlient.hentVedtak(vedtakId, MaskinportenTpContext(Tjenestepensjonnummer(""), "orgnr"))
        }
    }

    @Test
    fun `hent vedtaksliste for fnr, saktype og fomdato`() {
        val sakType = SakType.OMSTILLINGSSTOENAD
        val fomDato = LocalDate.now()
        val fnr = FNR
        val client =
            createHttpClient { request ->
                when (request.url.fullPath) {
                    "/api/samordning/vedtak?sakstype=$sakType&fomDato=$fomDato" ->
                        respond(
                            listOf(
                                vedtak(
                                    sakstype = sakType,
                                ),
                            ).toJson(),
                            HttpStatusCode.OK,
                            defaultHeaders,
                        )

                    else -> error("Unhandled ${request.url.fullPath}")
                }
            }

        val vedtaksvurderingKlient = VedtaksvurderingSamordningKlient(config, client)

        runBlocking {
            vedtaksvurderingKlient.hentVedtaksliste(
                sakType = sakType,
                fomDato = fomDato,
                fnr = fnr,
                MaskinportenTpContext(Tjenestepensjonnummer(""), "orgnr"),
            )
        }
    }

    private fun createHttpClient(handler: MockRequestHandler): HttpClient {
        val httpClient =
            HttpClient(MockEngine) {
                expectSuccess = true
                engine {
                    addHandler(handler)
                }
                install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            }
        return httpClient
    }
}
