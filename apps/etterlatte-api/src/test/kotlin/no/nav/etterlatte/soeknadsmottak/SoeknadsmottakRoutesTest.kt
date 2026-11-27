package no.nav.etterlatte.soeknadsmottak

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.kotest.matchers.shouldBe
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.etterlatte.ktor.runServerWithConfig
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.CLIENT_ID
import no.nav.etterlatte.ktor.token.issueSystembrukerToken
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.soeknadsmottak.SoeknadOverleveringResponse
import no.nav.etterlatte.libs.ktor.token.Issuer
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SoeknadsmottakRoutesTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private val klient = mockk<SoeknadsmottakKlient>()

    private val body =
        """{"mottaksnoekkel":"innsendt-soeknad:1","soeknadType":"UTVIDET_OMSTILLINGSSTOENAD","sakId":1,""" +
            """"journalpostId":"jp","dokumentInfoIder":["1"]}"""

    @BeforeAll
    fun before() = mockOAuth2Server.startRandomPort()

    @AfterAll
    fun after() = mockOAuth2Server.shutdown()

    @Test
    fun `krever rollen soeknadsmottak og videreformidler svar og feilstatus fra behandling`() {
        val config = config()
        coEvery { klient.overlever(match { it.journalpostId == "jp" }) } returns
            SoeknadOverleveringResponse("innsendt-soeknad:1", SakId(1), "jp", "oppgave")
        testApplication {
            runServerWithConfig(applicationConfig = config) { soeknadsmottakRoutes(klient) }

            suspend fun kall(
                roller: List<String>,
                innhold: String = body,
            ) = client.post("api/soeknad/mottak/overlevering") {
                header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSystembrukerToken(roles = roller)}")
                contentType(ContentType.Application.Json)
                setBody(innhold)
            }

            kall(listOf("les-oms-sak")).status shouldBe HttpStatusCode.Unauthorized
            kall(listOf(SOEKNADSMOTTAK_ROLLE)).let {
                it.status shouldBe HttpStatusCode.OK
                objectMapper.readTree(it.bodyAsText())["oppgaveId"].asString() shouldBe "oppgave"
            }

            coEvery { klient.overlever(match { it.journalpostId == "annen" }) } throws
                ForespoerselException(status = 409, code = "SOEKNADSMOTTAK_BEHANDLING_FEIL", detail = "Motstridende")
            kall(listOf(SOEKNADSMOTTAK_ROLLE), body.replace("\"jp\"", "\"annen\"")).status shouldBe HttpStatusCode.Conflict
            coVerify(exactly = 2) { klient.overlever(any()) }
        }
    }

    private fun config(): Config =
        ConfigFactory.parseMap(
            mapOf(
                "no.nav.security.jwt.issuers" to
                    listOf(
                        mapOf(
                            "discoveryurl" to
                                "http://localhost:${mockOAuth2Server.config.httpServer.port()}/${Issuer.AZURE.issuerName}/.well-known/openid-configuration",
                            "issuer_name" to Issuer.AZURE.issuerName,
                            "accepted_audience" to CLIENT_ID,
                        ),
                    ),
            ),
        )
}
