package no.nav.etterlatte.beregning

import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.grunnlag.Reduksjon
import no.nav.etterlatte.beregning.regler.FNR_1
import no.nav.etterlatte.beregning.regler.FNR_2
import no.nav.etterlatte.beregning.regler.MAKS_TRYGDETID
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.klienter.TrygdetidKlient
import no.nav.etterlatte.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.*
import java.util.UUID.randomUUID

internal class BeregnBarnepensjonServiceTest {

    private val vilkaarsvurderingKlient = mockk<VilkaarsvurderingKlient>()
    private val grunnlagKlient = mockk<GrunnlagKlientImpl>()
    private val beregningsGrunnlagService = mockk<BeregningsGrunnlagService>()
    private val trygdetidKlient = mockk<TrygdetidKlient>()
    private val featureToggleService = mockk<FeatureToggleService>()

    @BeforeEach
    fun setup() {
        every {
            featureToggleService.isEnabled(
                BeregnBarnepensjonServiceFeatureToggle.BrukInstitusjonsoppholdIBeregning,
                false
            )
        } returns false
        every {
            featureToggleService.isEnabled(BeregnBarnepensjonServiceFeatureToggle.BrukFaktiskTrygdetid, false)
        } returns false
        every {
            featureToggleService.isEnabled(BeregnBarnepensjonServiceFeatureToggle.BrukNyttRegelverkIBeregning, false)
        } returns false
    }

    private fun beregnBarnepensjonService(): BeregnBarnepensjonService {
        return BeregnBarnepensjonService(
            grunnlagKlient = grunnlagKlient,
            vilkaarsvurderingKlient = vilkaarsvurderingKlient,
            beregningsGrunnlagService = beregningsGrunnlagService,
            trygdetidKlient = trygdetidKlient,
            featureToggleService = featureToggleService
        )
    }

    @Test
    fun `skal beregne barnepensjon foerstegangsbehandling - ingen soesken`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any(),
                any()
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns null

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 2
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe BP_BELOEP_INGEN_SOESKEN_JAN_23
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2023, Month.APRIL)
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
    fun `skal beregne barnepensjon foerstegangsbehandling - ingen soesken - med trygdetid`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any(),
                any()
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)
        every {
            featureToggleService.isEnabled(BeregnBarnepensjonServiceFeatureToggle.BrukFaktiskTrygdetid, false)
        } returns true

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 2
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe BP_BELOEP_INGEN_SOESKEN_JAN_23
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2023, Month.APRIL)
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_23
                    soeskenFlokk shouldBe emptyList()
                    trygdetid shouldBe MAKS_TRYGDETID
                    regelResultat shouldNotBe null
                    regelVersjon shouldNotBe null
                }
                beregningsperioder.filter { p -> YearMonth.of(2023, 10).equals(p.datoFOM) } shouldBe emptyList()
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
                any(),
                any()
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, listOf(FNR_1))
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns null

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 2
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe BP_BELOEP_ETT_SOESKEN_JAN_23
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2023, Month.APRIL)
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_23
                    soeskenFlokk shouldBe listOf(FNR_1)
                    trygdetid shouldBe MAKS_TRYGDETID
                }
            }
        }
    }

    @Test
    fun `skal beregne barnepensjon foerstegangsbehandling - ett soesken - med trygdetid`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any(),
                any()
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, listOf(FNR_1))
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)
        every {
            featureToggleService.isEnabled(BeregnBarnepensjonServiceFeatureToggle.BrukFaktiskTrygdetid, false)
        } returns true

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 2
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe BP_BELOEP_ETT_SOESKEN_JAN_23
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2023, Month.APRIL)
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
                any(),
                any()
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, listOf(FNR_1, FNR_2))
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns null

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 2
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe BP_BELOEP_TO_SOESKEN_JAN_23
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2023, Month.APRIL)
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_23
                    soeskenFlokk shouldBe listOf(FNR_1, FNR_2)
                    trygdetid shouldBe MAKS_TRYGDETID
                }
            }
        }
    }

    @Test
    fun `skal beregne barnepensjon foerstegangsbehandling - to soesken - med trygdetid`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any(),
                any()
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, listOf(FNR_1, FNR_2))
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)
        every {
            featureToggleService.isEnabled(BeregnBarnepensjonServiceFeatureToggle.BrukFaktiskTrygdetid, false)
        } returns true

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 2
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe BP_BELOEP_TO_SOESKEN_JAN_23
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2023, Month.APRIL)
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
                any(),
                any()
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns mockk {
            every { resultat?.utfall } returns VilkaarsvurderingUtfall.OPPFYLT
        }
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns null

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 2
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe BP_BELOEP_INGEN_SOESKEN_JAN_23
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2023, Month.APRIL)
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_23
                    soeskenFlokk shouldBe emptyList()
                    trygdetid shouldBe MAKS_TRYGDETID
                }
            }
        }
    }

    @Test
    fun `skal beregne barnepensjon revurdering - ingen soesken - med trygdetid`() {
        val behandling = mockBehandling(BehandlingType.REVURDERING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any(),
                any()
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns mockk {
            every { resultat?.utfall } returns VilkaarsvurderingUtfall.OPPFYLT
        }
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)
        every {
            featureToggleService.isEnabled(BeregnBarnepensjonServiceFeatureToggle.BrukFaktiskTrygdetid, false)
        } returns true

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 2
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe BP_BELOEP_INGEN_SOESKEN_JAN_23
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2023, Month.APRIL)
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
                any(),
                any()
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns mockk {
            every { resultat?.utfall } returns VilkaarsvurderingUtfall.IKKE_OPPFYLT
        }
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns null

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 1
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
    fun `skal opphoere ved revurdering og vilkaarsvurdering ikke oppfylt - med trygdetid`() {
        val behandling = mockBehandling(BehandlingType.REVURDERING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any(),
                any()
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns mockk {
            every { resultat?.utfall } returns VilkaarsvurderingUtfall.IKKE_OPPFYLT
        }
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)
        every {
            featureToggleService.isEnabled(BeregnBarnepensjonServiceFeatureToggle.BrukFaktiskTrygdetid, false)
        } returns true

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 1
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
                any(),
                any()
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns null

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 1
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
    fun `skal sette beloep til 0 ved manuelt opphoer - med trygdetid`() {
        val behandling = mockBehandling(BehandlingType.MANUELT_OPPHOER)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any(),
                any()
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)
        every {
            featureToggleService.isEnabled(BeregnBarnepensjonServiceFeatureToggle.BrukFaktiskTrygdetid, false)
        } returns true

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 1
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
    fun `skal beregne barnepensjon foerstegangsbehandling - ingen soesken - med trygdetid og nytt regelverk`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any(),
                any()
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)
        every {
            featureToggleService.isEnabled(BeregnBarnepensjonServiceFeatureToggle.BrukFaktiskTrygdetid, false)
        } returns true
        every {
            featureToggleService.isEnabled(BeregnBarnepensjonServiceFeatureToggle.BrukNyttRegelverkIBeregning, false)
        } returns true

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 2
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe BP_BELOEP_INGEN_SOESKEN_JAN_23
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2023, Month.APRIL)
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_23
                    soeskenFlokk shouldBe emptyList()
                    trygdetid shouldBe MAKS_TRYGDETID
                    regelResultat shouldNotBe null
                    regelVersjon shouldNotBe null
                }
                with(beregningsperioder.single { p -> YearMonth.of(2023, 5).equals(p.datoFOM) }) {
                    utbetaltBeloep shouldBe BP_BELOEP_INGEN_SOESKEN_MAI_23
                    grunnbelopMnd shouldBe GRUNNBELOEP_MAI_23
                }
                with(beregningsperioder.single { p -> YearMonth.of(2023, 10).equals(p.datoFOM) }) {
                    utbetaltBeloep shouldBe BP_BELOEP_NYTT_REGELVERK
                    grunnbelopMnd shouldBe GRUNNBELOEP_MAI_23
                }
            }
        }
    }

    private fun barnepensjonBeregningsGrunnlag(behandlingId: UUID, soesken: List<String>) =
        BeregningsGrunnlag(
            behandlingId,
            mockk {
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
            ),
            institusjonsoppholdBeregningsgrunnlag = listOf(
                GrunnlagMedPeriode(
                    fom = LocalDate.of(2022, 8, 1),
                    tom = null,
                    data = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.NEI_KORT_OPPHOLD)
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

    private fun mockTrygdetid(behandlingId_: UUID): TrygdetidDto =
        mockk<TrygdetidDto>().apply {
            every { id } returns randomUUID()
            every { behandlingId } returns behandlingId_
            every { beregnetTrygdetid } returns mockk {
                every { total } returns BeregnOmstillingsstoenadServiceTest.TRYGDETID_40_AAR
                every { tidspunkt } returns Tidspunkt.now()
            }
        }

    companion object {
        val VIRKNINGSTIDSPUNKT_JAN_23: YearMonth = YearMonth.of(2023, Month.JANUARY)
        const val GRUNNBELOEP_JAN_23: Int = 9290
        const val GRUNNBELOEP_MAI_23: Int = 9885
        const val BP_BELOEP_INGEN_SOESKEN_JAN_23: Int = 3716
        const val BP_BELOEP_ETT_SOESKEN_JAN_23: Int = 3019
        const val BP_BELOEP_TO_SOESKEN_JAN_23: Int = 2787
        const val BP_BELOEP_INGEN_SOESKEN_MAI_23: Int = 3954
        const val BP_BELOEP_NYTT_REGELVERK: Int = GRUNNBELOEP_MAI_23
    }
}