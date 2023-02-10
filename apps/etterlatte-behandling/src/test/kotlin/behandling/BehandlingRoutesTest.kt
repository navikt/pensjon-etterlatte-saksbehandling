package behandling

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.log
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.behandlingRoutes
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingService
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerService
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.grunnlagsOpplysningMedPersonopplysning
import no.nav.etterlatte.libs.common.behandling.BehandlingMedGrunnlagsopplysninger
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.ktor.Saksbehandler
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.personOpplysning
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingRoutesTest {

    private val mockOAuth2Server = MockOAuth2Server()
    private val generellBehandlingService = mockk<GenerellBehandlingService>()
    private val foerstegangsbehandlingService = mockk<FoerstegangsbehandlingService>()
    private val revurderingService = mockk<RevurderingService>()
    private val manueltOpphoerService = mockk<ManueltOpphoerService>()

    @BeforeAll
    fun before() {
        mockOAuth2Server.start(1234)
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @AfterAll
    fun after() {
        mockOAuth2Server.shutdown()
    }

    @Test
    fun `kan lagre virkningstidspunkt som er maaned etter doedsfall og maks tre aar foer mottatt soeknad`() {
        val bodyVirkningstidspunkt = Instant.parse("2017-02-01T00:00:00Z")
        val bodyBegrunnelse = "begrunnelse"

        val soeknadMottatt = LocalDateTime.parse("2020-01-01T00:00:00.000000000")
        val doedsdato = LocalDate.parse("2016-12-30")

        mockBehandlingService(soeknadMottatt, doedsdato, bodyVirkningstidspunkt, bodyBegrunnelse)

        testApplication {
            application {
                restModule(this.log) {
                    behandlingRoutes(
                        generellBehandlingService,
                        foerstegangsbehandlingService,
                        revurderingService,
                        manueltOpphoerService
                    )
                }
            }

            val client = createClient {
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(objectMapper))
                }
            }

            val response = client.post("/api/behandling/$behandlingId/virkningstidspunkt") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                    "dato":"$bodyVirkningstidspunkt",
                    "begrunnelse":"$bodyBegrunnelse"
                    }
                    """.trimIndent()
                )
            }

            assertEquals(200, response.status.value)
        }
    }

    @Test
    fun `faar valideringsfeil hvis virkningstidspunkt er foer en maaned etter doedsfall`() {
        val bodyVirkningstidspunkt = Instant.parse("2020-01-01T00:00:00Z")
        val bodyBegrunnelse = "begrunnelse"

        val soeknadMottatt = LocalDateTime.parse("2020-02-01T00:00:00.000000000")
        val doedsdato = LocalDate.parse("2020-01-01")

        mockBehandlingService(soeknadMottatt, doedsdato, bodyVirkningstidspunkt, bodyBegrunnelse)

        testApplication {
            application {
                restModule(this.log) {
                    behandlingRoutes(
                        generellBehandlingService,
                        foerstegangsbehandlingService,
                        revurderingService,
                        manueltOpphoerService
                    )
                }
            }

            val client = createClient {
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(objectMapper))
                }
            }

            val response = client.post("/api/behandling/$behandlingId/virkningstidspunkt") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                    "dato":"$bodyVirkningstidspunkt",
                    "begrunnelse":"$bodyBegrunnelse"
                    }
                    """.trimIndent()
                )
            }

            assertEquals(400, response.status.value)
        }
    }

    @Test
    fun `faar valideringsfeil hvis virkningstidspunkt er tre aar foer mottatt soeknad`() {
        val bodyVirkningstidspunkt = Instant.parse("2017-01-01T00:00:00Z")
        val bodyBegrunnelse = "begrunnelse"

        val soeknadMottatt = LocalDateTime.parse("2020-01-01T00:00:00.000000000")
        val doedsdato = LocalDate.parse("2016-12-30")

        mockBehandlingService(soeknadMottatt, doedsdato, bodyVirkningstidspunkt, bodyBegrunnelse)

        testApplication {
            application {
                restModule(this.log) {
                    behandlingRoutes(
                        generellBehandlingService,
                        foerstegangsbehandlingService,
                        revurderingService,
                        manueltOpphoerService
                    )
                }
            }

            val client = createClient {
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(objectMapper))
                }
            }

            val response = client.post("/api/behandling/$behandlingId/virkningstidspunkt") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                    "dato":"$bodyVirkningstidspunkt",
                    "begrunnelse":"$bodyBegrunnelse"
                    }
                    """.trimIndent()
                )
            }

            assertEquals(400, response.status.value)
        }
    }

    private fun mockBehandlingService(
        soeknadMottatt: LocalDateTime,
        doedsdato: LocalDate?,
        bodyVirkningstidspunkt: Instant,
        bodyBegrunnelse: String
    ) {
        val behandlingMedDoedsdato = BehandlingMedGrunnlagsopplysninger(
            id = behandlingId,
            soeknadMottattDato = soeknadMottatt,
            personopplysning = grunnlagsOpplysningMedPersonopplysning(personOpplysning(doedsdato))
        )
        coEvery {
            generellBehandlingService.hentBehandlingMedEnkelPersonopplysning(
                behandlingId,
                Saksbehandler(NAVident),
                token,
                Opplysningstype.DOEDSDATO
            )
        } returns behandlingMedDoedsdato

        val parsetVirkningstidspunkt = YearMonth.from(
            LocalDate.ofInstant(bodyVirkningstidspunkt, norskTidssone).let {
                YearMonth.of(it.year, it.month)
            }
        )
        val virkningstidspunkt = Virkningstidspunkt(
            parsetVirkningstidspunkt,
            Grunnlagsopplysning.Saksbehandler(NAVident, Tidspunkt.now().instant),
            bodyBegrunnelse
        )
        every {
            foerstegangsbehandlingService.lagreVirkningstidspunkt(
                behandlingId,
                parsetVirkningstidspunkt,
                NAVident,
                bodyBegrunnelse

            )
        } returns virkningstidspunkt
    }

    private val token: String by lazy {
        mockOAuth2Server.issueToken(
            issuerId = ISSUER_ID,
            audience = CLIENT_ID,
            claims = mapOf(
                "navn" to "John Doe",
                "NAVident" to NAVident
            )
        ).serialize()
    }

    private companion object {
        val behandlingId: UUID = UUID.randomUUID()
        const val NAVident = "Saksbehandler01"
        const val ISSUER_ID = "azure"
        const val CLIENT_ID = "mock-client-id"
    }
}