package no.nav.etterlatte.beregning

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.beregning.klienter.BehandlingKlientImpl
import no.nav.etterlatte.beregning.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.beregning.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
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
            beregnetDato shouldNotBe null
            grunnlagMetadata shouldBe grunnlag.metadata
            beregningsperioder.size shouldBe 1
            with(beregningsperioder.first()) {
                delytelsesId shouldBe "BP"
                type shouldBe Beregningstyper.GP
                utbetaltBeloep shouldBe 3716
                datoFOM shouldBe behandling.virkningstidspunkt?.dato
                datoTOM shouldBe null
                grunnbelop shouldBe 111477
                grunnbelopMnd shouldBe 9290
                soeskenFlokk shouldBe emptyList()
                trygdetid shouldBe 40
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
            beregnetDato shouldNotBe null
            grunnlagMetadata shouldBe grunnlag.metadata
            beregningsperioder.size shouldBe 1
            with(beregningsperioder.first()) {
                delytelsesId shouldBe "BP"
                type shouldBe Beregningstyper.GP
                utbetaltBeloep shouldBe 3019
                datoFOM shouldBe behandling.virkningstidspunkt?.dato
                datoTOM shouldBe null
                grunnbelop shouldBe 111477
                grunnbelopMnd shouldBe 9290
                soeskenFlokk shouldBe listOf(FNR_1)
                trygdetid shouldBe 40
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
            beregnetDato shouldNotBe null
            grunnlagMetadata shouldBe grunnlag.metadata
            beregningsperioder.size shouldBe 1
            with(beregningsperioder.first()) {
                delytelsesId shouldBe "BP"
                type shouldBe Beregningstyper.GP
                utbetaltBeloep shouldBe 3716
                datoFOM shouldBe behandling.virkningstidspunkt?.dato
                datoTOM shouldBe null
                grunnbelop shouldBe 111477
                grunnbelopMnd shouldBe 9290
                soeskenFlokk shouldBe emptyList()
                trygdetid shouldBe 40
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
            beregnetDato shouldNotBe null
            grunnlagMetadata shouldBe grunnlag.metadata
            beregningsperioder.size shouldBe 1
            with(beregningsperioder.first()) {
                delytelsesId shouldBe "BP"
                type shouldBe Beregningstyper.GP
                utbetaltBeloep shouldBe 0
                datoFOM shouldBe behandling.virkningstidspunkt?.dato
                datoTOM shouldBe null
                grunnbelop shouldBe 111477
                grunnbelopMnd shouldBe 9290
                soeskenFlokk shouldBe emptyList()
                trygdetid shouldBe 40
            }
        }
    }

    @Test
    fun `skal sette beloep til 0 paafoelgende mnd etter doedsfall ved manuelt opphoer`() {
        val vilkaarsvurdering = vilkaarsvurdering(oppfylt = true)
        val behandling = behandling(BehandlingType.MANUELT_OPPHOER)
        val grunnlag = grunnlag(soesken = emptyList())

        val doedsdato = requireNotNull(grunnlag.hentAvdoed().hentDoedsdato()?.verdi)
        val virkningstidspunkt = YearMonth.from(doedsdato).plusMonths(1)

        val beregning = beregningService.beregnBarnepensjon(grunnlag, behandling, vilkaarsvurdering)

        with(beregning) {
            beregningId shouldNotBe null
            behandlingId shouldBe behandling.id
            beregnetDato shouldNotBe null
            grunnlagMetadata shouldBe grunnlag.metadata
            beregningsperioder.size shouldBe 1
            with(beregningsperioder.first()) {
                delytelsesId shouldBe "BP"
                type shouldBe Beregningstyper.GP
                utbetaltBeloep shouldBe 0
                datoFOM shouldBe virkningstidspunkt
                datoTOM shouldBe null
                grunnbelop shouldBe 111477
                grunnbelopMnd shouldBe 9290
                soeskenFlokk shouldBe emptyList()
                trygdetid shouldBe 40
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

    private fun behandling(type: BehandlingType): DetaljertBehandling =
        mockk<DetaljertBehandling>().apply {
            every { id } returns randomUUID()
            every { behandlingType } returns type
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt()
        }

    private fun vilkaarsvurdering(oppfylt: Boolean) =
        if (oppfylt) VilkaarsvurderingTestData.oppfylt else VilkaarsvurderingTestData.ikkeOppfylt

    companion object {
        const val FNR_1 = "11057523044"
    }
}