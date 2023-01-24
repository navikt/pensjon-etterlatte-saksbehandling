package beregning

import beregning.regler.FNR_1
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
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.beregning.Beregning
import no.nav.etterlatte.beregning.BeregningRepository
import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.beregning.BeregningServiceTest
import no.nav.etterlatte.beregning.beregning
import no.nav.etterlatte.beregning.klienter.BehandlingKlient
import no.nav.etterlatte.beregning.klienter.GrunnlagKlient
import no.nav.etterlatte.beregning.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.beregning.DelytelseId
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Beregningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.libs.testdata.vilkaarsvurdering.VilkaarsvurderingTestData
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.YearMonth
import java.util.*
import java.util.UUID.randomUUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BeregningRoutesTest {

    private val server = MockOAuth2Server()
    private val beregningRepository = mockk<BeregningRepository>()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val grunnlagKlient = mockk<GrunnlagKlient>()
    private val vilkaarsvurderingKlient = mockk<VilkaarsvurderingKlient>()

    private lateinit var beregningService: BeregningService

    @BeforeAll
    fun before() {
        server.start()
        System.setProperty("AZURE_APP_WELL_KNOWN_URL", server.wellKnownUrl(ISSUER_ID).toString())
        System.setProperty("AZURE_APP_CLIENT_ID", CLIENT_ID)

        beregningService =
            BeregningService(beregningRepository, vilkaarsvurderingKlient, grunnlagKlient, behandlingKlient)
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    @Test
    fun `skal returnere 404 naar beregning ikke finnes`() {
        every { beregningRepository.hent(any()) } returns null

        testApplication {
            application { restModule(this.log) { beregning(beregningService) } }

            val response = client.get("/api/beregning/${randomUUID()}") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `skal hente beregning`() {
        val beregning = beregning()

        every { beregningRepository.hent(beregning.behandlingId) } returns beregning

        testApplication {
            application { restModule(this.log) { beregning(beregningService) } }

            val response = client.get("/api/beregning/${beregning.behandlingId}") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val hentetBeregning = objectMapper.readValue(response.bodyAsText(), BeregningDTO::class.java)

            hentetBeregning shouldNotBe null
        }
    }

    @Test
    fun `skal opprette ny beregning for foerstegangsbehandling`() {
        val behandlingId = randomUUID()

        coEvery { behandlingKlient.beregn(behandlingId, any(), false) } returns true
        coEvery { behandlingKlient.beregn(behandlingId, any(), true) } returns true
        coEvery {
            behandlingKlient.hentBehandling(behandlingId, any())
        } returns behandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag(emptyList())
        coEvery {
            vilkaarsvurderingKlient.hentVilkaarsvurdering(behandlingId, any())
        } returns VilkaarsvurderingTestData.oppfylt
        every { beregningRepository.lagreEllerOppdaterBeregning(any()) } returnsArgument 0

        testApplication {
            application { restModule(this.log) { beregning(beregningService) } }

            val response = client.post("/api/beregning/$behandlingId") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val opprettetBeregning = objectMapper.readValue(response.bodyAsText(), BeregningDTO::class.java)

            response.status shouldBe HttpStatusCode.OK
            with(opprettetBeregning) {
                beregningsperioder shouldHaveSize 1
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe 3716
                }
            }
        }
    }

    private fun beregning(
        behandlingId: UUID = randomUUID(),
        datoFOM: YearMonth = YearMonth.of(2021, 2)
    ) =
        Beregning(
            beregningId = randomUUID(),
            behandlingId = behandlingId,
            beregnetDato = Tidspunkt.now(),
            grunnlagMetadata = no.nav.etterlatte.libs.common.grunnlag.Metadata(1, 1),
            beregningsperioder = listOf(
                Beregningsperiode(
                    delytelsesId = DelytelseId.BP,
                    type = Beregningstyper.GP,
                    datoFOM = datoFOM,
                    datoTOM = null,
                    utbetaltBeloep = 3000,
                    soeskenFlokk = listOf(FNR_1),
                    grunnbelopMnd = 10_000,
                    grunnbelop = 100_000,
                    trygdetid = 40
                )
            )
        )

    private fun grunnlag(soesken: List<String>) = GrunnlagTestData(
        opplysningsmapSakOverrides = mapOf(
            Opplysningstype.SOESKEN_I_BEREGNINGEN to Opplysning.Konstant(
                randomUUID(),
                kilde,
                Beregningsgrunnlag(
                    soesken.map {
                        SoeskenMedIBeregning(Foedselsnummer.of(it), true)
                    }
                ).toJsonNode()
            )
        )
    ).hentOpplysningsgrunnlag()

    private fun behandling(
        type: BehandlingType,
        virk: YearMonth = BeregningServiceTest.VIRKNINGSTIDSPUNKT_JAN_23
    ): DetaljertBehandling =
        mockk<DetaljertBehandling>().apply {
            every { id } returns randomUUID()
            every { behandlingType } returns type
            every { sak } returns 1
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt(virk)
        }

    private val token: String by lazy {
        server.issueToken(
            issuerId = ISSUER_ID,
            audience = CLIENT_ID,
            claims = mapOf("navn" to "John Doe", "NAVident" to "Saksbehandler01")
        ).serialize()
    }

    private companion object {
        const val ISSUER_ID = "azure"
        const val CLIENT_ID = "azure-id for saksbehandler"
    }
}