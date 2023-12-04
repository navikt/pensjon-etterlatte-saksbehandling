package no.nav.etterlatte.behandling.brevoppsett

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.module
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BrevoppsettRoutesTest {
    private val applicationContext: ApplicationContext = mockk(relaxed = true)
    private var brevoppsettDao: BrevoppsettDao = mockk()
    private var behandlingService: BehandlingService = mockk()

    private val server: MockOAuth2Server = MockOAuth2Server()
    private lateinit var hoconApplicationConfig: HoconApplicationConfig

    @BeforeAll
    fun before() {
        server.start()
        val httpServer = server.config.httpServer
        hoconApplicationConfig = buildTestApplicationConfigurationForOauth(httpServer.port(), AZURE_ISSUER, CLIENT_ID)

        brevoppsettDao = mockk()
        every { applicationContext.tilgangService } returns
            mockk {
                every { harTilgangTilBehandling(any(), any()) } returns true
                every { harTilgangTilSak(any(), any()) } returns true
            }
        every { applicationContext.brevoppsettService } returns BrevoppsettService(brevoppsettDao, behandlingService)
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    @Test
    fun `skal opprette brevoppsett`() {
        val behandlingId = UUID.randomUUID()
        val opprettDto = brevoppsettDto()

        every { behandlingService.hentBehandling(any()) } returns behandling(behandlingId)
        every { brevoppsettDao.lagre(any()) } returns brevoppsett(behandlingId)

        testApplication {
            environment { config = hoconApplicationConfig }
            application { module(applicationContext) }

            val client = createClient()

            val response =
                client.post("/api/behandling/${UUID.randomUUID()}/brevoppsett") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(brevoppsettDto())
                }

            val opprettetBrevoppsett: BrevoppsettDto = response.body()
            response.status shouldBe HttpStatusCode.Created

            opprettetBrevoppsett.aldersgruppe shouldBe opprettDto.aldersgruppe
            opprettetBrevoppsett.brevtype shouldBe opprettDto.brevtype
            opprettetBrevoppsett.etterbetaling shouldBe opprettDto.etterbetaling
            opprettetBrevoppsett.kilde shouldNotBe null
        }
    }

    @Test
    fun `skal oppdatere brevoppsett`() {
        val behandlingId = UUID.randomUUID()
        val oppdaterDto = brevoppsettDto()

        every { behandlingService.hentBehandling(any()) } returns behandling(behandlingId)
        every { brevoppsettDao.lagre(any()) } returns brevoppsett(behandlingId)

        testApplication {
            environment { config = hoconApplicationConfig }
            application { module(applicationContext) }

            val client = createClient()

            val response =
                client.put("/api/behandling/${UUID.randomUUID()}/brevoppsett") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(brevoppsettDto())
                }

            val opprettetBrevoppsett: BrevoppsettDto = response.body()
            response.status shouldBe HttpStatusCode.OK

            opprettetBrevoppsett.aldersgruppe shouldBe oppdaterDto.aldersgruppe
            opprettetBrevoppsett.brevtype shouldBe oppdaterDto.brevtype
            opprettetBrevoppsett.etterbetaling shouldBe oppdaterDto.etterbetaling
            opprettetBrevoppsett.kilde shouldNotBe null
        }
    }

    @Test
    fun `skal hente brevoppsett`() {
        val behandlingId = UUID.randomUUID()
        val opprettDto = brevoppsettDto()

        every { behandlingService.hentBehandling(any()) } returns behandling(behandlingId)
        every { brevoppsettDao.hent(any()) } returns brevoppsett(behandlingId)

        testApplication {
            environment { config = hoconApplicationConfig }
            application { module(applicationContext) }

            val client = createClient()

            val response =
                client.get("/api/behandling/${UUID.randomUUID()}/brevoppsett") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val hentetBrevoppsett: BrevoppsettDto = response.body()
            response.status shouldBe HttpStatusCode.OK

            hentetBrevoppsett.brevtype shouldBe opprettDto.brevtype
        }
    }

    private fun behandling(behandlingId: UUID): Behandling =
        mockk {
            every { id } returns behandlingId
            every { status } returns BehandlingStatus.BEREGNET
            every { virkningstidspunkt } returns
                Virkningstidspunkt.create(YearMonth.of(2023, 1), "ident", "begrunnelse")
        }

    private fun brevoppsett(behandlingId: UUID) =
        Brevoppsett(
            behandlingId = behandlingId,
            etterbetaling = Etterbetaling(YearMonth.of(2023, 1), YearMonth.of(2023, 2)),
            brevtype = Brevtype.NASJONAL,
            aldersgruppe = Aldersgruppe.UNDER_18,
            kilde = Grunnlagsopplysning.Saksbehandler.create("Saksbehandler01"),
        )

    private fun brevoppsettDto() =
        BrevoppsettDto(
            etterbetaling =
                EtterbetalingDto(
                    datoFom = LocalDate.of(2023, 1, 1),
                    datoTom = LocalDate.of(2023, 2, 28),
                ),
            brevtype = Brevtype.NASJONAL,
            aldersgruppe = Aldersgruppe.UNDER_18,
            kilde = null,
        )

    private fun ApplicationTestBuilder.createClient() =
        createClient {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper))
            }
        }

    private val token: String by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims =
                mapOf(
                    "navn" to "John Doe",
                    "NAVident" to "Saksbehandler01",
                ),
        ).serialize()
    }

    private companion object {
        const val CLIENT_ID = "mock-client-id"
    }
}
