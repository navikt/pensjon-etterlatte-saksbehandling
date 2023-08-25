package no.nav.etterlatte.beregning.grunnlag

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.util.*
import java.util.UUID.randomUUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BeregningsGrunnlagRoutesTest {

    private val server = MockOAuth2Server()
    private lateinit var applicationConfig: HoconApplicationConfig
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val repository = mockk<BeregningsGrunnlagRepository>()
    private val service = BeregningsGrunnlagService(repository, behandlingKlient)

    @BeforeAll
    fun before() {
        server.start()

        applicationConfig =
            buildTestApplicationConfigurationForOauth(server.config.httpServer.port(), AZURE_ISSUER, CLIENT_ID)
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    @Test
    fun `skal returnere 204 naar beregnings ikke finnes`() {
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns DetaljertBehandling(
            id = randomUUID(),
            sak = 123,
            sakType = SakType.BARNEPENSJON,
            behandlingOpprettet = LocalDateTime.now(),
            soeknadMottattDato = null,
            innsender = null,
            soeker = "diam",
            gjenlevende = listOf(),
            avdoed = listOf(),
            soesken = listOf(),
            status = BehandlingStatus.VILKAARSVURDERT,
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            virkningstidspunkt = null,
            revurderingsaarsak = null,
            prosesstype = Prosesstype.MANUELL,
            boddEllerArbeidetUtlandet = null,
            revurderingInfo = null,
            enhet = "1111"
        )

        every { repository.finnBarnepensjonGrunnlagForBehandling(any()) } returns null

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            val response = client.get("/api/beregning/beregningsgrunnlag/${randomUUID()}/barnepensjon") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            response.status shouldBe HttpStatusCode.NoContent
        }
    }

    @Test
    fun `skal hente beregningsgrunnlag for sist iverksatte hvis ikke noe finnes og det er revurdering`() {
        val idRevurdering = randomUUID()
        val idForrigeIverksatt = randomUUID()
        val sakId = 123L
        val virkRevurdering = Virkningstidspunkt(
            dato = YearMonth.of(2023, Month.JANUARY),
            kilde = Grunnlagsopplysning.Saksbehandler(
                ident = "",
                tidspunkt = Tidspunkt.now()
            ),
            begrunnelse = ""

        )
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(idRevurdering, any()) } returns DetaljertBehandling(
            id = randomUUID(),
            sak = sakId,
            sakType = SakType.BARNEPENSJON,
            behandlingOpprettet = LocalDateTime.now(),
            soeknadMottattDato = null,
            innsender = null,
            soeker = "",
            gjenlevende = listOf(),
            avdoed = listOf(),
            soesken = listOf(),
            status = BehandlingStatus.VILKAARSVURDERT,
            behandlingType = BehandlingType.REVURDERING,
            virkningstidspunkt = virkRevurdering,
            revurderingsaarsak = null,
            prosesstype = Prosesstype.MANUELL,
            boddEllerArbeidetUtlandet = null,
            revurderingInfo = null,
            enhet = "1111"
        )
        coEvery {
            behandlingKlient.hentSisteIverksatteBehandling(sakId, any())
        } returns SisteIverksatteBehandling(idForrigeIverksatt)
        every { repository.finnBarnepensjonGrunnlagForBehandling(idRevurdering) } returns null
        every { repository.finnBarnepensjonGrunnlagForBehandling(idForrigeIverksatt) } returns BeregningsGrunnlag(
            behandlingId = idForrigeIverksatt,
            kilde = Grunnlagsopplysning.Saksbehandler(
                ident = "",
                tidspunkt = Tidspunkt.now()
            ),
            soeskenMedIBeregning = listOf(),
            institusjonsoppholdBeregningsgrunnlag = emptyList()
        )

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            val response = client.get("/api/beregning/beregningsgrunnlag/$idRevurdering/barnepensjon") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            response.status shouldBe HttpStatusCode.OK
        }

        coVerify(exactly = 1) {
            repository.finnBarnepensjonGrunnlagForBehandling(idRevurdering)
            repository.finnBarnepensjonGrunnlagForBehandling(idForrigeIverksatt)
            behandlingKlient.hentSisteIverksatteBehandling(sakId, any())
        }
    }

    @Test
    fun `skal hente beregning`() {
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true

        val id = randomUUID()

        every { repository.finnBarnepensjonGrunnlagForBehandling(any()) } returns BeregningsGrunnlag(
            id,
            Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
            emptyList(),
            emptyList()
        )

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            val response = client.get("/api/beregning/beregningsgrunnlag/$id/barnepensjon") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val hentet = objectMapper.readValue(response.bodyAsText(), BeregningsGrunnlag::class.java)

            hentet.soeskenMedIBeregning shouldBe emptyList()
        }
    }

    @Test
    fun `skal returnere not found naar saksbehandler ikke har tilgang til behandling`() {
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns false

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            client.get("/api/beregning/beregningsgrunnlag/${randomUUID()}/barnepensjon") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }.let {
                it.status shouldBe HttpStatusCode.NotFound
            }
        }
    }

    @Test
    fun `skal returnere not found naar saksbehandler ikke har tilgang til behandling ved opprettelse`() {
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns false

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            val client = createClient {
                install(ContentNegotiation) {
                    jackson { registerModule(JavaTimeModule()) }
                }
            }

            client.post("/api/beregning/beregningsgrunnlag/${randomUUID()}/barnepensjon") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(
                    BarnepensjonBeregningsGrunnlag(
                        emptyList(),
                        emptyList()
                    )
                )
            }.let {
                it.status shouldBe HttpStatusCode.NotFound
            }
        }
    }

    @Test
    fun `skal oppretter`() {
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
        coEvery { behandlingKlient.beregn(any(), any(), any()) } returns true
        every { repository.finnBarnepensjonGrunnlagForBehandling(any()) } returns null
        every { repository.lagre(any()) } returns true
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns DetaljertBehandling(
            id = randomUUID(),
            sak = 123,
            sakType = SakType.BARNEPENSJON,
            behandlingOpprettet = LocalDateTime.now(),
            soeknadMottattDato = null,
            innsender = null,
            soeker = "diam",
            gjenlevende = listOf(),
            avdoed = listOf(),
            soesken = listOf(),
            status = BehandlingStatus.VILKAARSVURDERT,
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2023, 1),
                kilde = Grunnlagsopplysning.Saksbehandler(
                    ident = "",
                    tidspunkt = Tidspunkt.now()
                ),
                ""
            ),
            revurderingsaarsak = null,
            prosesstype = Prosesstype.MANUELL,
            boddEllerArbeidetUtlandet = null,
            revurderingInfo = null,
            enhet = "1111"
        )

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            val client = createClient {
                install(ContentNegotiation) {
                    jackson { registerModule(JavaTimeModule()) }
                }
            }

            client.post("/api/beregning/beregningsgrunnlag/${randomUUID()}/barnepensjon") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(
                    BarnepensjonBeregningsGrunnlag(
                        emptyList(),
                        emptyList()
                    )
                )
            }.let {
                it.status shouldBe HttpStatusCode.NoContent
            }
        }
    }

    @Test
    fun `skal returnere conflict fra opprettelse `() {
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
        coEvery { behandlingKlient.beregn(any(), any(), any()) } returns true
        every { repository.finnBarnepensjonGrunnlagForBehandling(any()) } returns null
        every { repository.lagre(any()) } returns false
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns DetaljertBehandling(
            id = randomUUID(),
            sak = 123,
            sakType = SakType.BARNEPENSJON,
            behandlingOpprettet = LocalDateTime.now(),
            soeknadMottattDato = null,
            innsender = null,
            soeker = "diam",
            gjenlevende = listOf(),
            avdoed = listOf(),
            soesken = listOf(),
            status = BehandlingStatus.VILKAARSVURDERT,
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2023, 1),
                kilde = Grunnlagsopplysning.Saksbehandler(
                    ident = "",
                    tidspunkt = Tidspunkt.now()
                ),
                ""
            ),
            revurderingsaarsak = null,
            prosesstype = Prosesstype.MANUELL,
            boddEllerArbeidetUtlandet = null,
            revurderingInfo = null,
            enhet = "1111"
        )

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            val client = createClient {
                install(ContentNegotiation) {
                    jackson { registerModule(JavaTimeModule()) }
                }
            }

            client.post("/api/beregning/beregningsgrunnlag/${randomUUID()}/barnepensjon") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(
                    BarnepensjonBeregningsGrunnlag(
                        emptyList(),
                        emptyList()
                    )
                )
            }.let {
                it.status shouldBe HttpStatusCode.Conflict
            }
        }
    }

    @Test
    fun `skal duplisere hvis en saksbehandler kaller duplisering`() {
        val forrige = randomUUID()
        val nye = randomUUID()

        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
        coEvery { behandlingKlient.beregn(any(), any(), any()) } returns true
        every { repository.finnBarnepensjonGrunnlagForBehandling(forrige) } returns BeregningsGrunnlag(
            forrige,
            Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
            emptyList(),
            emptyList()
        )
        every { repository.finnBarnepensjonGrunnlagForBehandling(nye) } returns null
        every { repository.lagre(any()) } returns true

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            client.post("/api/beregning/beregningsgrunnlag/$nye/fra/$forrige") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }.let {
                it.status shouldBe HttpStatusCode.NoContent
            }
        }
    }

    @Test
    fun `skal duplisere hvis en system bruker kaller duplisering`() {
        val forrige = randomUUID()
        val nye = randomUUID()

        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
        coEvery { behandlingKlient.beregn(any(), any(), any()) } returns true
        every { repository.finnBarnepensjonGrunnlagForBehandling(forrige) } returns BeregningsGrunnlag(
            forrige,
            Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
            emptyList(),
            emptyList()
        )
        every { repository.finnBarnepensjonGrunnlagForBehandling(nye) } returns null
        every { repository.lagre(any()) } returns true

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            client.post("/api/beregning/beregningsgrunnlag/$nye/fra/$forrige") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $systemToken")
            }.let {
                it.status shouldBe HttpStatusCode.NoContent
            }
        }
    }

    @Test
    fun `skal feile hvis forrige beregningsgrunnlag ikke finnes`() {
        val forrige = randomUUID()
        val nye = randomUUID()

        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
        coEvery { behandlingKlient.beregn(any(), any(), any()) } returns true
        every { repository.finnBarnepensjonGrunnlagForBehandling(forrige) } returns null
        every { repository.lagre(any()) } returns true

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            client.post("/api/beregning/beregningsgrunnlag/$nye/fra/$forrige") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $systemToken")
            }.let {
                it.status shouldBe HttpStatusCode.InternalServerError
            }
        }
    }

    @Test
    fun `skal feile hvis nye beregningsgrunnlag allerede finnes`() {
        val forrige = randomUUID()
        val nye = randomUUID()

        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
        coEvery { behandlingKlient.beregn(any(), any(), any()) } returns true
        every { repository.finnBarnepensjonGrunnlagForBehandling(forrige) } returns BeregningsGrunnlag(
            forrige,
            Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
            emptyList(),
            emptyList()
        )
        every { repository.finnBarnepensjonGrunnlagForBehandling(nye) } returns BeregningsGrunnlag(
            nye,
            Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
            emptyList(),
            emptyList()
        )
        every { repository.lagre(any()) } returns true

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            client.post("/api/beregning/beregningsgrunnlag/$nye/fra/$forrige") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $systemToken")
            }.let {
                it.status shouldBe HttpStatusCode.InternalServerError
            }
        }
    }

    private val token: String by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims = mapOf("navn" to "John Doe", "NAVident" to "Saksbehandler01")
        ).serialize()
    }

    private val systemToken: String by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims = mapOf("oid" to "woot", "sub" to "woot")
        ).serialize()
    }

    private companion object {
        const val CLIENT_ID = "azure-id for saksbehandler"
    }
}