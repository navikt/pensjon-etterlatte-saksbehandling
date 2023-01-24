package no.nav.etterlatte.beregning

import beregning.regler.FNR_1
import beregning.regler.FNR_2
import beregning.regler.MAKS_TRYGDETID
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.beregning.klienter.BehandlingKlientImpl
import no.nav.etterlatte.beregning.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.beregning.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Beregningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.libs.testdata.vilkaarsvurdering.VilkaarsvurderingTestData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID.randomUUID

internal class BeregningServiceTest {

    private val beregningRepository = mockk<BeregningRepository>()
    private val vilkaarsvurderingKlient = mockk<VilkaarsvurderingKlient>()
    private val grunnlagKlient = mockk<GrunnlagKlientImpl>()
    private val behandlingKlient = mockk<BehandlingKlientImpl>()
    private lateinit var beregningService: BeregningService

    @BeforeEach
    fun setup() {
        beregningService = BeregningService(
            beregningRepository = beregningRepository,
            vilkaarsvurderingKlient = vilkaarsvurderingKlient,
            grunnlagKlient = grunnlagKlient,
            behandlingKlient = behandlingKlient
        )
    }

    @Test
    fun `skal beregne barnepensjon foerstegangsbehandling - ingen soesken`() {
        val vilkaarsvurdering = vilkaarsvurdering(oppfylt = true)
        val behandling = behandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = grunnlag(soesken = emptyList())

        val beregning = beregningService.beregnBarnepensjon(grunnlag, behandling, vilkaarsvurdering)

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

    @Test
    fun `skal beregne barnepensjon foerstegangsbehandling - ett soesken`() {
        val vilkaarsvurdering = vilkaarsvurdering(oppfylt = true)
        val behandling = behandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = grunnlag(soesken = listOf(FNR_1))

        val beregning = beregningService.beregnBarnepensjon(grunnlag, behandling, vilkaarsvurdering)

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

    @Test
    fun `skal beregne barnepensjon foerstegangsbehandling - to soesken`() {
        val vilkaarsvurdering = vilkaarsvurdering(oppfylt = true)
        val behandling = behandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = grunnlag(soesken = listOf(FNR_1, FNR_2))

        val beregning = beregningService.beregnBarnepensjon(grunnlag, behandling, vilkaarsvurdering)

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

    @Test
    fun `skal beregne barnepensjon revurdering - ingen soesken`() {
        val vilkaarsvurdering = vilkaarsvurdering(oppfylt = true)
        val behandling = behandling(BehandlingType.REVURDERING)
        val grunnlag = grunnlag(soesken = emptyList())

        val beregning = beregningService.beregnBarnepensjon(grunnlag, behandling, vilkaarsvurdering)

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

    @Test
    fun `skal sette beloep til 0 ved revurdering og vilkaar ikke oppfylt`() {
        val vilkaarsvurdering = vilkaarsvurdering(oppfylt = false)
        val behandling = behandling(BehandlingType.REVURDERING)
        val grunnlag = grunnlag(soesken = emptyList())

        val beregning = beregningService.beregnBarnepensjon(grunnlag, behandling, vilkaarsvurdering)

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
                soeskenFlokk shouldBe emptyList()
                trygdetid shouldBe MAKS_TRYGDETID
            }
        }
    }

    @Test
    fun `skal sette beloep til 0 ved manuelt opphoer`() {
        val behandling = behandling(BehandlingType.MANUELT_OPPHOER)
        val grunnlag = grunnlag(soesken = emptyList())

        val beregning = beregningService.beregnManueltOpphoerBarnepensjon(grunnlag, behandling)

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
                soeskenFlokk shouldBe emptyList()
                trygdetid shouldBe MAKS_TRYGDETID
            }
        }
    }

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

    private fun behandling(type: BehandlingType, virk: YearMonth = VIRKNINGSTIDSPUNKT_JAN_23): DetaljertBehandling =
        mockk<DetaljertBehandling>().apply {
            every { id } returns randomUUID()
            every { behandlingType } returns type
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt(virk)
        }

    private fun vilkaarsvurdering(oppfylt: Boolean) =
        if (oppfylt) VilkaarsvurderingTestData.oppfylt else VilkaarsvurderingTestData.ikkeOppfylt

    companion object {
        val VIRKNINGSTIDSPUNKT_JAN_23: YearMonth = YearMonth.of(2023, 1)
        const val GRUNNBELOEP_JAN_23: Int = 9290
        const val BP_BELOEP_INGEN_SOESKEN_JAN_23: Int = 3716
        const val BP_BELOEP_ETT_SOESKEN_JAN_23: Int = 3019
        const val BP_BELOEP_TO_SOESKEN_JAN_23: Int = 2787
    }
}