package no.nav.etterlatte.beregning

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.BeregnBarnepensjonServiceFeatureToggle.BrukFaktiskTrygdetid
import no.nav.etterlatte.beregning.BeregnBarnepensjonServiceFeatureToggle.BrukInstitusjonsoppholdIBeregning
import no.nav.etterlatte.beregning.BeregnBarnepensjonServiceFeatureToggle.BrukNyttRegelverkIBeregning
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.grunnlag.Reduksjon
import no.nav.etterlatte.beregning.regler.FNR_1
import no.nav.etterlatte.beregning.regler.FNR_2
import no.nav.etterlatte.beregning.regler.MAKS_TRYGDETID
import no.nav.etterlatte.beregning.regler.barnepensjon.BP_2024_DATO
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.beregning.regler.toGrunnlag
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.klienter.TrygdetidKlient
import no.nav.etterlatte.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle.AVDOED
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import java.util.UUID.randomUUID

internal class BeregnBarnepensjonServiceTest {
    private val vilkaarsvurderingKlient = mockk<VilkaarsvurderingKlient>()
    private val grunnlagKlient = mockk<GrunnlagKlientImpl>()
    private val beregningsGrunnlagService = mockk<BeregningsGrunnlagService>()
    private val trygdetidKlient = mockk<TrygdetidKlient>()
    private val featureToggleService = DummyFeatureToggleService()

    @BeforeEach
    fun setup() {
        featureToggleService.settBryter(BrukInstitusjonsoppholdIBeregning, false)
        featureToggleService.settBryter(BrukFaktiskTrygdetid, false)
        featureToggleService.settBryter(BrukNyttRegelverkIBeregning, false)
    }

    private fun beregnBarnepensjonService(): BeregnBarnepensjonService {
        return BeregnBarnepensjonService(
            grunnlagKlient = grunnlagKlient,
            vilkaarsvurderingKlient = vilkaarsvurderingKlient,
            beregningsGrunnlagService = beregningsGrunnlagService,
            trygdetidKlient = trygdetidKlient,
            featureToggleService = featureToggleService,
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
                any(),
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
                any(),
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)
        featureToggleService.settBryter(BrukFaktiskTrygdetid, true)

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
                beregningsperioder.filter { p -> BP_2024_DATO.equals(p.datoFOM) } shouldBe emptyList()
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
                any(),
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
                any(),
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, listOf(FNR_1))
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)
        featureToggleService.settBryter(BrukFaktiskTrygdetid, true)

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
                any(),
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
                any(),
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, listOf(FNR_1, FNR_2))
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)
        featureToggleService.settBryter(BrukFaktiskTrygdetid, true)

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
                any(),
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns
            mockk {
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
                any(),
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns
            mockk {
                every { resultat?.utfall } returns VilkaarsvurderingUtfall.OPPFYLT
            }
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)
        featureToggleService.settBryter(BrukFaktiskTrygdetid, true)

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
                any(),
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns
            mockk {
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
                any(),
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns
            mockk {
                every { resultat?.utfall } returns VilkaarsvurderingUtfall.IKKE_OPPFYLT
            }
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)
        featureToggleService.settBryter(BrukFaktiskTrygdetid, true)

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
                any(),
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
                any(),
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)
        featureToggleService.settBryter(BrukFaktiskTrygdetid, true)

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
    fun `skal beregne barnepensjon foerstegangsbehandling - ett soesken - med trygdetid og nytt regelverk`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns
            beregningsGrunnlagMedSoesken(
                behandling.id,
                Triple(YearMonth.of(2023, 1), YearMonth.of(2023, 3), listOf(FNR_1, FNR_2)),
                Triple(YearMonth.of(2023, 4), null, listOf(FNR_1)),
            )
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)
        featureToggleService.settBryter(BrukFaktiskTrygdetid, true)
        featureToggleService.settBryter(BrukNyttRegelverkIBeregning, true)

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 3
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe BP_BELOEP_TO_SOESKEN_JAN_23
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2023, Month.MARCH)
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_23
                    soeskenFlokk shouldBe listOf(FNR_1, FNR_2)
                    trygdetid shouldBe MAKS_TRYGDETID
                    regelResultat shouldNotBe null
                    regelVersjon shouldNotBe null
                }
                with(beregningsperioder.single { p -> YearMonth.of(2023, 4).equals(p.datoFOM) }) {
                    utbetaltBeloep shouldBe BP_BELOEP_ETT_SOESKEN_JAN_23
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_23
                }
                with(beregningsperioder.single { p -> YearMonth.of(2023, 5).equals(p.datoFOM) }) {
                    utbetaltBeloep shouldBe BP_BELOEP_ETT_SOESKEN_MAI_23
                    grunnbelopMnd shouldBe GRUNNBELOEP_MAI_23
                }
                with(beregningsperioder.single { p -> YearMonth.of(2023, 10).equals(p.datoFOM) }) {
                    utbetaltBeloep shouldBe BP_BELOEP_NYTT_REGELVERK_EN_DOED_FORELDER
                    grunnbelopMnd shouldBe GRUNNBELOEP_MAI_23
                }
            }
        }
    }

    @Test
    fun `skal beregne barnepensjon foerstegangsbehandling - med flere avdøde foreldre og nytt regelverk`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns
            grunnlagMedEkstraAvdoedForelder(LocalDate.of(2023, 11, 12))
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns null
        featureToggleService.settBryter(BrukNyttRegelverkIBeregning, true)

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker)
            beregning.beregningsperioder.size shouldBeGreaterThanOrEqual 4

            with(beregning.beregningsperioder[0]) {
                datoFOM shouldBe YearMonth.of(2023, 1)
                datoTOM shouldBe YearMonth.of(2023, 4)
            }
            with(beregning.beregningsperioder[1]) {
                datoFOM shouldBe YearMonth.of(2023, 5)
                datoTOM shouldBe YearMonth.of(2023, 9)
            }
            with(beregning.beregningsperioder[2]) {
                datoFOM shouldBe YearMonth.of(2023, 10)
                datoTOM shouldBe YearMonth.of(2023, 11)
                utbetaltBeloep shouldBe BP_BELOEP_NYTT_REGELVERK_EN_DOED_FORELDER
            }
            with(beregning.beregningsperioder[3]) {
                datoFOM shouldBe YearMonth.of(2023, 12)
                datoTOM shouldBe null
                utbetaltBeloep shouldBe BP_BELOEP_NYTT_REGELVERK_TO_DOEDE_FORELDRE
            }
        }
    }

    private fun grunnlagMedEkstraAvdoedForelder(doedsdato: LocalDate): Grunnlag {
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val nyligAvdoedFoedselsnummer = Folkeregisteridentifikator.of("15447924940")
        val nyligAvdoed: List<Grunnlagsdata<JsonNode>> =
            listOf(
                mapOf(
                    Opplysningstype.DOEDSDATO to konstantOpplysning(doedsdato),
                    Opplysningstype.PERSONROLLE to konstantOpplysning(AVDOED),
                    Opplysningstype.FOEDSELSNUMMER to konstantOpplysning(nyligAvdoedFoedselsnummer),
                ),
            )
        return Grunnlag(
            grunnlag.soeker,
            grunnlag.familie + nyligAvdoed,
            grunnlag.sak,
            grunnlag.metadata,
        )
    }

    private fun <T : Any> konstantOpplysning(a: T): Opplysning.Konstant<JsonNode> {
        val kilde = Grunnlagsopplysning.Pdl(Tidspunkt.now(), "", "")
        return Opplysning.Konstant(randomUUID(), kilde, a.toJsonNode())
    }

    private fun barnepensjonBeregningsGrunnlag(
        behandlingId: UUID,
        soesken: List<String>,
    ) = BeregningsGrunnlag(
        behandlingId,
        defaultKilde(),
        soeskenMedIBeregning =
            listOf(
                GrunnlagMedPeriode(
                    fom = VIRKNINGSTIDSPUNKT_JAN_23.minusMonths(1).atDay(1),
                    tom = null,
                    data =
                        soesken.map {
                            SoeskenMedIBeregning(
                                Folkeregisteridentifikator.of(it),
                                skalBrukes = true,
                            )
                        },
                ),
            ),
        institusjonsoppholdBeregningsgrunnlag = defaultInstitusjonsopphold(),
        beregningsMetode = BeregningsMetode.NASJONAL.toGrunnlag(),
    )

    private fun beregningsGrunnlagMedSoesken(
        behandlingId: UUID,
        vararg soesken: Triple<YearMonth, YearMonth?, List<String>>,
    ) = BeregningsGrunnlag(
        behandlingId,
        defaultKilde(),
        soeskenMedIBeregning =
            soesken.map {
                GrunnlagMedPeriode(
                    fom = it.first.atDay(1),
                    tom = it.second?.atEndOfMonth(),
                    data =
                        it.third.map {
                            SoeskenMedIBeregning(
                                Folkeregisteridentifikator.of(it),
                                skalBrukes = true,
                            )
                        },
                )
            },
        institusjonsoppholdBeregningsgrunnlag = defaultInstitusjonsopphold(),
        beregningsMetode = BeregningsMetode.NASJONAL.toGrunnlag(),
    )

    private fun defaultKilde(): Grunnlagsopplysning.Saksbehandler =
        mockk {
            every { ident } returns "Z123456"
            every { tidspunkt } returns Tidspunkt.now()
            every { type } returns ""
        }

    private fun defaultInstitusjonsopphold() =
        listOf(
            GrunnlagMedPeriode(
                fom = LocalDate.of(2022, 8, 1),
                tom = null,
                data = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.NEI_KORT_OPPHOLD),
            ),
        )

    private fun mockBehandling(
        type: BehandlingType,
        virk: YearMonth = VIRKNINGSTIDSPUNKT_JAN_23,
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
            every { beregnetTrygdetid } returns
                mockk {
                    every { resultat } returns
                        mockk {
                            every { samletTrygdetidNorge } returns BeregnOmstillingsstoenadServiceTest.TRYGDETID_40_AAR
                        }
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
        const val BP_BELOEP_ETT_SOESKEN_MAI_23: Int = 3213
        const val BP_BELOEP_NYTT_REGELVERK_EN_DOED_FORELDER: Int = GRUNNBELOEP_MAI_23
        const val BP_BELOEP_NYTT_REGELVERK_TO_DOEDE_FORELDRE: Int = 22_241
    }
}
