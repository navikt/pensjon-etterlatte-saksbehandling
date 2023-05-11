package no.nav.etterlatte.beregning

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.beregning.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.beregning.regler.FNR_1
import no.nav.etterlatte.beregning.regler.FNR_2
import no.nav.etterlatte.beregning.regler.MAKS_TRYGDETID
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.*
import java.util.UUID.randomUUID

internal class BeregnBarnepensjonServiceTest {

    private val vilkaarsvurderingKlient = mockk<VilkaarsvurderingKlient>()
    private val grunnlagKlient = mockk<GrunnlagKlientImpl>()
    private val beregningsGrunnlagService = mockk<BeregningsGrunnlagService>()
    private lateinit var beregnBarnepensjonService: BeregnBarnepensjonService

    @BeforeEach
    fun setup() {
        beregnBarnepensjonService = BeregnBarnepensjonService(
            grunnlagKlient = grunnlagKlient,
            vilkaarsvurderingKlient = vilkaarsvurderingKlient,
            beregningsGrunnlagService = beregningsGrunnlagService
        )
    }

    @Test
    fun `skal beregne barnepensjon foerstegangsbehandling - ingen soesken`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any()
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())

        runBlocking {
            val beregning = beregnBarnepensjonService.beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBe 1
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe BP_BELOEP_INGEN_SOESKEN_JAN_23
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe null
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_23
                    soeskenFlokk shouldBe emptyList()
                    trygdetid shouldBe MAKS_TRYGDETID
                    regelResultat shouldNotBe null
                    regelVersjon shouldNotBe null
                }
            }
        }
    }

    @Test
    fun `skal beregne barnepensjon foerstegangsbehandling - ett soesken`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any()
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, listOf(FNR_1))

        runBlocking {
            val beregning = beregnBarnepensjonService.beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBe 1
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe BP_BELOEP_ETT_SOESKEN_JAN_23
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe null
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_23
                    soeskenFlokk shouldBe listOf(FNR_1)
                    trygdetid shouldBe MAKS_TRYGDETID
                }
            }
        }
    }

    @Test
    fun `skal beregne barnepensjon foerstegangsbehandling - to soesken`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any()
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, listOf(FNR_1, FNR_2))

        runBlocking {
            val beregning = beregnBarnepensjonService.beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBe 1
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe BP_BELOEP_TO_SOESKEN_JAN_23
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe null
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_23
                    soeskenFlokk shouldBe listOf(FNR_1, FNR_2)
                    trygdetid shouldBe MAKS_TRYGDETID
                }
            }
        }
    }

    @Test
    fun `skal beregne barnepensjon revurdering - ingen soesken`() {
        val behandling = mockBehandling(BehandlingType.REVURDERING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any()
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns mockk {
            every { resultat?.utfall } returns VilkaarsvurderingUtfall.OPPFYLT
        }

        runBlocking {
            val beregning = beregnBarnepensjonService.beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBe 1
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe BP_BELOEP_INGEN_SOESKEN_JAN_23
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe null
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_23
                    soeskenFlokk shouldBe emptyList()
                    trygdetid shouldBe MAKS_TRYGDETID
                }
            }
        }
    }

    @Test
    fun `skal opphoere ved revurdering og vilkaarsvurdering ikke oppfylt`() {
        val behandling = mockBehandling(BehandlingType.REVURDERING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any()
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns mockk {
            every { resultat?.utfall } returns VilkaarsvurderingUtfall.IKKE_OPPFYLT
        }

        runBlocking {
            val beregning = beregnBarnepensjonService.beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBe 1
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe 0
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe null
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_23
                    soeskenFlokk shouldBe null
                    trygdetid shouldBe 0
                }
            }
        }
    }

    @Test
    fun `skal sette beloep til 0 ved manuelt opphoer`() {
        val behandling = mockBehandling(BehandlingType.MANUELT_OPPHOER)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any()
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())

        runBlocking {
            val beregning = beregnBarnepensjonService.beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBe 1
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe 0
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe null
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_23
                    soeskenFlokk shouldBe null
                    trygdetid shouldBe 0
                }
            }
        }
    }

    private fun barnepensjonBeregningsGrunnlag(behandlingId: UUID, soesken: List<String>) =
        BeregningsGrunnlag(
            behandlingId,
            mockk<Grunnlagsopplysning.Saksbehandler> {
                every { ident } returns "Z123456"
                every { tidspunkt } returns Tidspunkt.now()
                every { type } returns ""
            },
            soeskenMedIBeregning = listOf(
                GrunnlagMedPeriode(
                    fom = VIRKNINGSTIDSPUNKT_JAN_23.minusMonths(1).atDay(1),
                    tom = null,
                    data = soesken.map {
                        SoeskenMedIBeregning(
                            Folkeregisteridentifikator.of(it),
                            skalBrukes = true
                        )
                    }
                )
            )

        )

    private fun mockBehandling(
        type: BehandlingType,
        virk: YearMonth = VIRKNINGSTIDSPUNKT_JAN_23
    ): DetaljertBehandling =
        mockk<DetaljertBehandling>().apply {
            every { id } returns randomUUID()
            every { sak } returns 1
            every { behandlingType } returns type
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt(virk)
        }

    companion object {
        val VIRKNINGSTIDSPUNKT_JAN_23: YearMonth = YearMonth.of(2023, 1)
        const val GRUNNBELOEP_JAN_23: Int = 9290
        const val BP_BELOEP_INGEN_SOESKEN_JAN_23: Int = 3716
        const val BP_BELOEP_ETT_SOESKEN_JAN_23: Int = 3019
        const val BP_BELOEP_TO_SOESKEN_JAN_23: Int = 2787
    }
}