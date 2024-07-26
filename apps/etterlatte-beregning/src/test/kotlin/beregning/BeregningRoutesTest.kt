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
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.beregning.OverstyrBeregningDTO
import no.nav.etterlatte.libs.common.beregning.OverstyrtBeregningKategori
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.sanksjon.SanksjonService
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.YearMonth
import java.util.UUID
import java.util.UUID.randomUUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BeregningRoutesTest {
    private val server = MockOAuth2Server()
    private val beregningRepository = mockk<BeregningRepository>()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val beregnBarnepensjonService = mockk<BeregnBarnepensjonService>()
    private val beregnOmstillingsstoenadService = mockk<BeregnOmstillingsstoenadService>()
    private val beregnOverstyrBeregningService = mockk<BeregnOverstyrBeregningService>()
    private val sanksjonService = mockk<SanksjonService>()
    private val beregningService =
        BeregningService(
            beregningRepository = beregningRepository,
            behandlingKlient = behandlingKlient,
            beregnBarnepensjonService = beregnBarnepensjonService,
            beregnOmstillingsstoenadService = beregnOmstillingsstoenadService,
            beregnOverstyrBeregningService = beregnOverstyrBeregningService,
            sanksjonService = sanksjonService,
        )

    @BeforeAll
    fun before() {
        server.start()

        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) } returns true
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    @Test
    fun `skal returnere 404 naar beregning ikke finnes`() {
        every { beregningRepository.hent(any()) } returns null

        testApplication {
            runServer(server) {
                beregning(beregningService, behandlingKlient)
            }

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
        val behandling = mockk<DetaljertBehandling>()

        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) } returns true
        every { beregningRepository.hent(beregning.behandlingId) } returns beregning
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        every { behandling.sak } returns 1L
        every { beregningRepository.hentOverstyrBeregning(1L) } returns null

        testApplication {
            runServer(server) {
                beregning(beregningService, behandlingKlient)
            }

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
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) } returns false

        testApplication {
            runServer(server) {
                beregning(beregningService, behandlingKlient)
            }

            client
                .get("/api/beregning/${beregning.behandlingId}") {
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

        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns mockBehandling()
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) } returns true
        every { beregningRepository.hentOverstyrBeregning(1L) } returns null
        coEvery { beregnBarnepensjonService.beregn(any(), any()) } returns beregning
        every { beregningRepository.lagreEllerOppdaterBeregning(any()) } returnsArgument 0

        testApplication {
            runServer(server) {
                beregning(beregningService, behandlingKlient)
            }

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

    @Test
    fun `skal hente overstyrBeregning`() {
        val behandling = mockk<DetaljertBehandling>()

        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        every { behandling.sak } returns 1L
        every { beregningRepository.hentOverstyrBeregning(1L) } returns
            OverstyrBeregning(
                1L,
                "Test",
                Tidspunkt.now(),
                kategori = OverstyrtBeregningKategori.UKJENT_KATEGORI,
            )

        testApplication {
            runServer(server) {
                beregning(beregningService, behandlingKlient)
            }

            val response =
                client.get("/api/beregning/${randomUUID()}/overstyrt") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val hentetOverstyrBeregning =
                objectMapper.readValue(response.bodyAsText(), OverstyrBeregningDTO::class.java)

            hentetOverstyrBeregning shouldNotBe null
            hentetOverstyrBeregning.beskrivelse shouldBe "Test"
            hentetOverstyrBeregning.kategori shouldBe OverstyrtBeregningKategori.UKJENT_KATEGORI
        }
    }

    @Test
    fun `skal ikke hente overstyrBeregning hvis den ikke finnes`() {
        val behandling = mockk<DetaljertBehandling>()

        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        every { behandling.sak } returns 1L
        every { beregningRepository.hentOverstyrBeregning(1L) } returns null

        testApplication {
            runServer(server) {
                beregning(beregningService, behandlingKlient)
            }

            val response =
                client.get("/api/beregning/${randomUUID()}/overstyrt") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.NoContent
        }
    }

    private fun beregning(
        behandlingId: UUID = randomUUID(),
        datoFOM: YearMonth = YearMonth.of(2021, 2),
        overstyrBeregning: OverstyrBeregning? = null,
    ) = Beregning(
        beregningId = randomUUID(),
        behandlingId = behandlingId,
        type = Beregningstype.BP,
        beregnetDato = Tidspunkt.now(),
        grunnlagMetadata =
            no.nav.etterlatte.libs.common.grunnlag
                .Metadata(1, 1),
        beregningsperioder =
            listOf(
                Beregningsperiode(
                    datoFOM = datoFOM,
                    datoTOM = null,
                    utbetaltBeloep = 3000,
                    soeskenFlokk = listOf(HELSOESKEN_FOEDSELSNUMMER.value),
                    grunnbelopMnd = 10_000,
                    grunnbelop = 100_000,
                    trygdetid = 40,
                    kilde = Grunnlagsopplysning.RegelKilde("regelid", Tidspunkt.now(), "1"),
                ),
            ),
        overstyrBeregning = overstyrBeregning,
    )

    private fun mockBehandling(): DetaljertBehandling =
        mockk<DetaljertBehandling>().apply {
            every { id } returns randomUUID()
            every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
            every { sak } returns 1
            every { sakType } returns SakType.BARNEPENSJON
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2023, 1))
            every { opphoerFraOgMed } returns null
        }

    private val token: String by lazy { server.issueSaksbehandlerToken() }
}
