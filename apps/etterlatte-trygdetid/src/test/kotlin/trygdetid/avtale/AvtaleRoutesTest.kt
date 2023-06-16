package no.nav.etterlatte.trygdetid.avtale

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.log
import no.nav.etterlatte.libs.common.trygdetid.avtale.TrygdetidAvtale
import no.nav.etterlatte.libs.common.trygdetid.avtale.TrygdetidAvtaleKriteria
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
import java.time.LocalDate
import java.time.Month
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AvtaleRoutesTest {

    private val service = AvtaleService()

    private val server = MockOAuth2Server()

    @BeforeAll
    fun beforeAll() {
        server.start()
    }

    @AfterAll
    fun afterAll() {
        server.shutdown()
    }

    @Test
    fun `skal levere informasjon om avtaler`() {
        testApplication(server.config.httpServer.port()) { client ->
            val response = client.get("/api/trygdetid/avtaler") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            response.status shouldBe HttpStatusCode.OK

            val avtaler = response.body<List<TrygdetidAvtale>>()

            avtaler.size shouldBe 26
            with(avtaler.first { it.kode == "EOS_NOR" }) {
                this.beskrivelse shouldBe "Eøs-Avtalen/Nordisk Konvensjon"
                this.fraDato shouldBe LocalDate.of(1899, Month.DECEMBER, 31)
            }

            with(avtaler.first { it.kode == "BIH" }) {
                this.beskrivelse shouldBe "Bosnia-Hercegovina"
                this.fraDato shouldBe LocalDate.of(1899, Month.DECEMBER, 31)

                val trygdetidDato = this.datoer.first()

                trygdetidDato.kode shouldBe "BIH1992"
                trygdetidDato.beskrivelse shouldBe "01.03.1992"
                trygdetidDato.fraDato shouldBe LocalDate.of(1899, Month.DECEMBER, 31)
            }
        }
    }

    @Test
    fun `skal levere informasjon om avtale kriterier`() {
        testApplication(server.config.httpServer.port()) { client ->
            val response = client.get("/api/trygdetid/avtaler/kriteria") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            response.status shouldBe HttpStatusCode.OK

            val avtaler = response.body<List<TrygdetidAvtaleKriteria>>()

            avtaler.size shouldBe 7
            with(avtaler.first { it.kode == "YRK_MEDL" }) {
                this.beskrivelse shouldBe "Yrkesaktiv i Norge eller EØS, ett års medlemskap i Norge"
                this.fraDato shouldBe LocalDate.of(1899, Month.DECEMBER, 31)
            }
        }
    }

    private fun testApplication(port: Int, block: suspend (client: HttpClient) -> Unit) {
        io.ktor.server.testing.testApplication {
            environment {
                config = buildTestApplicationConfigurationForOauth(port, AZURE_ISSUER, AZURE_CLIENT_ID)
            }
            application { restModule(log) { avtale(service) } }

            val client = createClient {
                install(ContentNegotiation) {
                    jackson { registerModule(JavaTimeModule()) }
                }
            }

            block(client)
        }
    }

    private val token: String by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = AZURE_CLIENT_ID,
            claims = mapOf("navn" to "John Doe", "NAVident" to "Saksbehandler01")
        ).serialize()
    }

    private companion object {
        const val AZURE_CLIENT_ID: String = "azure-id"
    }
}