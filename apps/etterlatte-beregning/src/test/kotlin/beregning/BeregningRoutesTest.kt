package no.nav.etterlatte.beregning

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.regler.FNR_1
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.klienter.TrygdetidKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
import java.time.YearMonth
import java.util.UUID
import java.util.UUID.randomUUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BeregningRoutesTest {
    private val server = MockOAuth2Server()
    private lateinit var applicationConfig: HoconApplicationConfig
    private val beregningRepository = mockk<BeregningRepository>()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val beregnBarnepensjonService = mockk<BeregnBarnepensjonService>()
    private val beregnOmstillingsstoenadService = mockk<BeregnOmstillingsstoenadService>()
    private val beregningsGrunnlagService = mockk<BeregningsGrunnlagService>()
    private val trygdetidKlient = mockk<TrygdetidKlient>()
    private val beregningService =
        BeregningService(
            beregningRepository = beregningRepository,
            behandlingKlient = behandlingKlient,
            beregnBarnepensjonService = beregnBarnepensjonService,
            beregnOmstillingsstoenadService = beregnOmstillingsstoenadService,
            beregningsGrunnlagService = beregningsGrunnlagService,
            trygdetidKlient = trygdetidKlient,
        )

    @BeforeAll
    fun before() {
        server.start()

        applicationConfig =
            buildTestApplicationConfigurationForOauth(server.config.httpServer.port(), AZURE_ISSUER, CLIENT_ID)
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    @Test
    fun `skal returnere 404 naar beregning ikke finnes`() {
        every { beregningRepository.hent(any()) } returns null

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregning(beregningService, behandlingKlient) } }

            val response =
                client.get("/api/beregning/${randomUUID()}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `skal hente beregning`() {
        val beregning = beregning()

        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
        every { beregningRepository.hent(beregning.behandlingId) } returns beregning

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregning(beregningService, behandlingKlient) } }

            val response =
                client.get("/api/beregning/${beregning.behandlingId}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val hentetBeregning = objectMapper.readValue(response.bodyAsText(), BeregningDTO::class.java)

            hentetBeregning shouldNotBe null
        }
    }

    @Test
    fun `skal returnere not found naar saksbehandler ikke har tilgang til behandling`() {
        val beregning = beregning()

        every { beregningRepository.hent(beregning.behandlingId) } returns beregning
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns false

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregning(beregningService, behandlingKlient) } }

            client.get("/api/beregning/${beregning.behandlingId}") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }.let {
                it.status shouldBe HttpStatusCode.NotFound
            }
        }
    }

    @Test
    fun `skal opprette ny beregning for foerstegangsbehandling av barnepensjon`() {
        val behandling = mockBehandling()
        val beregning = beregning()

        coEvery { behandlingKlient.beregn(any(), any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns mockBehandling()
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
        coEvery { beregnBarnepensjonService.beregn(any(), any()) } returns beregning
        every { beregningRepository.lagreEllerOppdaterBeregning(any()) } returnsArgument 0

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregning(beregningService, behandlingKlient) } }

            val response =
                client.post("/api/beregning/${behandling.id}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val opprettetBeregning = objectMapper.readValue(response.bodyAsText(), BeregningDTO::class.java)

            response.status shouldBe HttpStatusCode.OK
            with(opprettetBeregning) {
                beregningsperioder shouldHaveSize 1
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe 3000
                }
            }
        }
    }

    private fun beregning(
        behandlingId: UUID = randomUUID(),
        datoFOM: YearMonth = YearMonth.of(2021, 2),
    ) = Beregning(
        beregningId = randomUUID(),
        behandlingId = behandlingId,
        type = Beregningstype.BP,
        beregnetDato = Tidspunkt.now(),
        grunnlagMetadata = no.nav.etterlatte.libs.common.grunnlag.Metadata(1, 1),
        beregningsperioder =
            listOf(
                Beregningsperiode(
                    datoFOM = datoFOM,
                    datoTOM = null,
                    utbetaltBeloep = 3000,
                    soeskenFlokk = listOf(FNR_1),
                    grunnbelopMnd = 10_000,
                    grunnbelop = 100_000,
                    trygdetid = 40,
                    kilde = Grunnlagsopplysning.RegelKilde("regelid", Tidspunkt.now(), "1"),
                ),
            ),
    )

    private fun mockBehandling(): DetaljertBehandling =
        mockk<DetaljertBehandling>().apply {
            every { id } returns randomUUID()
            every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
            every { sak } returns 1
            every { sakType } returns SakType.BARNEPENSJON
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2023, 1))
        }

    private val token: String by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims = mapOf("navn" to "John Doe", "NAVident" to "Saksbehandler01"),
        ).serialize()
    }

    private companion object {
        const val CLIENT_ID = "azure-id for saksbehandler"
    }
}
