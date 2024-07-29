package no.nav.etterlatte.vedtaksvurdering

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.token.issueSystembrukerToken
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
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Month
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SamordningsvedtakRouteTest {
    private val server = MockOAuth2Server()
    private val vedtakSamordningService: VedtakSamordningService = mockk()

    @BeforeAll
    fun before() {
        server.start()
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
    fun `skal returnere vedtak naar token har noedvendig gruppe og vedtak eksisterer`() {
        coEvery { vedtakSamordningService.hentVedtak(1234) } returns
            samordningVedtak()

        testApplication {
            runServer(server) {
                samordningSystembrukerVedtakRoute(vedtakSamordningService)
            }

            val response =
                client.get("/api/samordning/vedtak/1234") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${token(listOf("dummy", "samordning-read"))}")
                }

            response.status shouldBe HttpStatusCode.OK
            coVerify { vedtakSamordningService.hentVedtak(1234) }
        }
    }

    private fun token(roles: List<String>): String = server.issueSystembrukerToken(mittsystem = "pensjon-pen", roles = roles)
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
