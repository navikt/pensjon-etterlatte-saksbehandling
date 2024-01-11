package vedtaksvurdering

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakSamordningDto
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.vedtaksvurdering.VedtakSamordningService
import no.nav.etterlatte.vedtaksvurdering.samordningsvedtakRoute
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
import java.time.Month
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SamordningsvedtakRouteTest {
    private val server = MockOAuth2Server()
    private lateinit var applicationConfig: HoconApplicationConfig
    private val vedtakSamordningService: VedtakSamordningService = mockk()

    @BeforeAll
    fun before() {
        server.start()

        applicationConfig =
            buildTestApplicationConfigurationForOauth(server.config.httpServer.port(), AZURE_ISSUER, CLIENT_ID)
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    @Test
    fun `skal returnere 401 naar token mangler noedvendig rolle`() {
        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { samordningsvedtakRoute(vedtakSamordningService) } }

            val response =
                client.get("/api/samordning/vedtak/1234") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${token(listOf("dummy"))}")
                }

            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `skal returnere vedtak naar token har noedvendig rolle og vedtak eksisterer`() {
        coEvery { vedtakSamordningService.hentVedtak(1234) } returns
            samordningVedtak()

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { samordningsvedtakRoute(vedtakSamordningService) } }

            val response =
                client.get("/api/samordning/vedtak/1234") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${token(listOf("dummy", "samordning-read"))}")
                }

            response.status shouldBe HttpStatusCode.OK
            coVerify { vedtakSamordningService.hentVedtak(1234) }
        }
    }

    private fun token(roles: List<String>): String =
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims =
                mapOf(
                    "roles" to roles,
                    "sub" to "pensjon-pen",
                    "oid" to "pensjon-pen",
                ),
        ).serialize()

    private companion object {
        const val CLIENT_ID = "azure-id for app"
    }
}

private fun samordningVedtak() =
    VedtakSamordningDto(
        vedtakId = 123456L,
        fnr = FNR_2,
        status = VedtakStatus.IVERKSATT,
        virkningstidspunkt = YearMonth.of(2024, Month.JANUARY),
        sak = VedtakSak(FNR_2, SakType.OMSTILLINGSSTOENAD, id = 15L),
        behandling = Behandling(BehandlingType.REVURDERING, id = UUID.randomUUID()),
        type = VedtakType.ENDRING,
        vedtakFattet = VedtakFattet("SBH", "1014", Tidspunkt.now().minus(2, ChronoUnit.DAYS)),
        attestasjon = Attestasjon("SBH", "1014", Tidspunkt.now().minus(1, ChronoUnit.DAYS)),
        beregning = null,
        perioder = emptyList(),
    )
