package no.nav.etterlatte.beregning

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.BeregnBarnepensjonServiceFeatureToggle.BrukNyttRegelverkIBeregning
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.grunnlag.Reduksjon
import no.nav.etterlatte.beregning.regler.MAKS_TRYGDETID
import no.nav.etterlatte.beregning.regler.barnepensjon.BP_2024_DATO
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.beregning.regler.toGrunnlag
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.klienter.TrygdetidKlient
import no.nav.etterlatte.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.Vedtaksloesning
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
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.GrunnlagOpplysningerDto
import no.nav.etterlatte.libs.common.trygdetid.OpplysningerDifferanse
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.UKJENT_AVDOED
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.token.Systembruker
import org.junit.jupiter.api.Assertions
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
    fun `skal beregne barnepensjon foerstegangsbehandling - ingen soesken - nasjonal`() {
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
    fun `skal beregne barnepensjon foerstegangsbehandling - ingen soesken - prorata`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList(), BeregningsMetode.PRORATA)
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)

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
                    utbetaltBeloep shouldBe BP_BELOEP_INGEN_SOESKEN_JAN_23_PRORATA
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2023, Month.APRIL)
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_23
                    soeskenFlokk shouldBe emptyList()
                    trygdetid shouldBe PRORATA_TRYGDETID_30_AAR / 2
                    regelResultat shouldNotBe null
                    regelVersjon shouldNotBe null
                }
                beregningsperioder.filter { p -> BP_2024_DATO.equals(p.datoFOM) } shouldBe emptyList()
            }
        }
    }

    @Test
    fun `skal beregne barnepensjon foerstegangsbehandling - ingen soesken - best`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList(), BeregningsMetode.BEST)
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)

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
        } returns barnepensjonBeregningsGrunnlag(behandling.id, listOf(HELSOESKEN_FOEDSELSNUMMER))
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)

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
                    soeskenFlokk shouldBe listOf(HELSOESKEN_FOEDSELSNUMMER.value)
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
        } returns barnepensjonBeregningsGrunnlag(behandling.id, listOf(HELSOESKEN_FOEDSELSNUMMER, HELSOESKEN2_FOEDSELSNUMMER))
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)

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
                    soeskenFlokk shouldBe listOf(HELSOESKEN_FOEDSELSNUMMER.value, HELSOESKEN2_FOEDSELSNUMMER.value)
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
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)

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
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)

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
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)

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
    fun `skal beregne barnepensjon foerstegangsbehandling - ett soesken - med nytt regelverk`() {
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
                Triple(
                    YearMonth.of(2023, 1),
                    YearMonth.of(2023, 3),
                    listOf(HELSOESKEN_FOEDSELSNUMMER.value, HELSOESKEN2_FOEDSELSNUMMER.value),
                ),
                Triple(YearMonth.of(2023, 4), null, listOf(HELSOESKEN_FOEDSELSNUMMER.value)),
            )
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)
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
                    soeskenFlokk shouldBe listOf(HELSOESKEN_FOEDSELSNUMMER.value, HELSOESKEN2_FOEDSELSNUMMER.value)
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
                with(beregningsperioder.single { p -> YearMonth.of(2024, 1).equals(p.datoFOM) }) {
                    utbetaltBeloep shouldBe BP_BELOEP_NYTT_REGELVERK_EN_DOED_FORELDER
                    grunnbelopMnd shouldBe GRUNNBELOEP_MAI_23
                }
            }
        }
    }

    @Test
    fun `skal ikke beregne med knekkpunkt fra regelverksendring hvis nytt regelverk er avslått`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, virk = YearMonth.of(2023, Month.DECEMBER))
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(any(), any())
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns null
        featureToggleService.settBryter(BrukNyttRegelverkIBeregning, false)
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)

        runBlocking {
            val beregning =
                beregnBarnepensjonService().beregn(
                    behandling,
                    bruker,
                )
            Assertions.assertEquals(1, beregning.beregningsperioder.size)
            with(beregning.beregningsperioder[0]) {
                datoFOM shouldBe YearMonth.of(2023, Month.DECEMBER)
                datoTOM shouldBe null
            }
        }
    }

    @Test
    fun `skal beregne med knekkpunkt fra regelverksendring hvis nytt regelverk er avslått men er migrering`() {
        val behandling =
            mockBehandling(
                BehandlingType.FØRSTEGANGSBEHANDLING,
                virk = YearMonth.of(2023, Month.DECEMBER),
                vedtaksloesning = Vedtaksloesning.PESYS,
            )

        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(any(), any())
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns null
        featureToggleService.settBryter(BrukNyttRegelverkIBeregning, false)
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)

        runBlocking {
            val beregning =
                beregnBarnepensjonService().beregn(
                    behandling,
                    Systembruker("migrering", "migrering"),
                )
            beregning.beregningsperioder.size shouldBeGreaterThanOrEqual 3
            with(beregning.beregningsperioder[0]) {
                datoFOM shouldBe YearMonth.of(2023, Month.DECEMBER)
                datoTOM shouldBe YearMonth.of(2023, Month.DECEMBER)
            }
            with(beregning.beregningsperioder[1]) {
                datoFOM shouldBe YearMonth.of(2024, Month.JANUARY)
                datoTOM shouldBe YearMonth.of(2024, Month.APRIL)
            }
        }
    }

    @Test
    fun `skal beregne barnepensjon foerstegangsbehandling - med flere avdoede foreldre og nytt regelverk`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns
            grunnlagMedEkstraAvdoedForelder(LocalDate.of(2023, 11, 12))
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns
            barnepensjonBeregningsGrunnlag(
                behandling.id,
                emptyList(),
                institusjonsoppholdBeregningsgrunnlag = emptyList(),
            )
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns null
        featureToggleService.settBryter(BrukNyttRegelverkIBeregning, true)
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns mockTrygdetid(behandling.id)

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker)
            beregning.beregningsperioder.size shouldBeGreaterThanOrEqual 3

            with(beregning.beregningsperioder[0]) {
                datoFOM shouldBe YearMonth.of(2023, 1)
                datoTOM shouldBe YearMonth.of(2023, 4)
            }
            with(beregning.beregningsperioder[1]) {
                datoFOM shouldBe YearMonth.of(2023, 5)
                datoTOM shouldBe YearMonth.of(2023, 12)
            }
            with(beregning.beregningsperioder[2]) {
                datoFOM shouldBe YearMonth.of(2024, 1)
                datoTOM shouldBe YearMonth.of(2024, 4)
                utbetaltBeloep shouldBe BP_BELOEP_NYTT_REGELVERK_TO_DOEDE_FORELDRE
            }
        }
    }

    @Test
    fun `beregne foerstegangsbehandling med ukjent avdød`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, virk = YearMonth.of(2024, Month.JANUARY))
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns GrunnlagTestData().hentGrunnlagMedUkjentAvdoed()

        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns
            barnepensjonBeregningsGrunnlag(
                behandling.id,
                emptyList(),
                beregningsMetode = BeregningsMetode.BEST,
                virk = YearMonth.of(2024, Month.JANUARY).atDay(1),
            )

        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns
            TrygdetidDto(
                randomUUID(),
                ident = UKJENT_AVDOED,
                behandlingId = behandling.id,
                beregnetTrygdetid =
                    DetaljertBeregnetTrygdetidDto(
                        resultat = DetaljertBeregnetTrygdetidResultat.fraSamletTrygdetidProrata(40, null),
                        tidspunkt = Tidspunkt.now(),
                    ),
                trygdetidGrunnlag = emptyList(),
                opplysninger = GrunnlagOpplysningerDto(null, null, null, null),
                overstyrtNorskPoengaar = null,
                opplysningerDifferanse = OpplysningerDifferanse(false, mockk<GrunnlagOpplysningerDto>()),
            )
        featureToggleService.settBryter(BrukNyttRegelverkIBeregning, true)

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker)
            beregning.beregningsperioder.size shouldBeGreaterThanOrEqual 1

            with(beregning.beregningsperioder[0]) {
                datoFOM shouldBe YearMonth.of(2024, 1)
                datoTOM shouldBe YearMonth.of(2024, 4)
                utbetaltBeloep shouldBe GRUNNBELOEP_MAI_23
            }
        }
    }

    private fun grunnlagMedEkstraAvdoedForelder(doedsdato: LocalDate): Grunnlag {
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val nyligAvdoedFoedselsnummer = AVDOED2_FOEDSELSNUMMER
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
        soesken: List<Folkeregisteridentifikator>,
        beregningsMetode: BeregningsMetode = BeregningsMetode.NASJONAL,
        institusjonsoppholdBeregningsgrunnlag: List<GrunnlagMedPeriode<InstitusjonsoppholdBeregningsgrunnlag>> =
            defaultInstitusjonsopphold(),
        virk: LocalDate = VIRKNINGSTIDSPUNKT_JAN_23.minusMonths(1).atDay(1),
    ) = BeregningsGrunnlag(
        behandlingId,
        defaultKilde(),
        soeskenMedIBeregning =
            listOf(
                GrunnlagMedPeriode(
                    fom = virk,
                    tom = null,
                    data =
                        soesken.map {
                            SoeskenMedIBeregning(
                                it,
                                skalBrukes = true,
                            )
                        },
                ),
            ),
        institusjonsoppholdBeregningsgrunnlag = institusjonsoppholdBeregningsgrunnlag,
        beregningsMetode = beregningsMetode.toGrunnlag(),
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
                        it.third.map { fnr ->
                            SoeskenMedIBeregning(
                                Folkeregisteridentifikator.of(fnr),
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
        vedtaksloesning: Vedtaksloesning = Vedtaksloesning.GJENNY,
    ): DetaljertBehandling =
        mockk<DetaljertBehandling> {
            every { id } returns randomUUID()
            every { sak } returns 1
            every { behandlingType } returns type
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt(virk)
            every { kilde } returns vedtaksloesning
            every { revurderingsaarsak } returns null
        }

    private fun mockTrygdetid(behandlingId_: UUID): TrygdetidDto =
        mockk<TrygdetidDto>().apply {
            every { id } returns randomUUID()
            every { behandlingId } returns behandlingId_
            every { ident } returns AVDOED_FOEDSELSNUMMER.value
            every { beregnetTrygdetid } returns
                mockk {
                    every { resultat } returns
                        mockk {
                            every { samletTrygdetidNorge } returns TRYGDETID_40_AAR
                            every { samletTrygdetidTeoretisk } returns PRORATA_TRYGDETID_30_AAR
                            every { prorataBroek } returns PRORATA_BROEK
                        }
                    every { tidspunkt } returns Tidspunkt.now()
                }
        }

    companion object {
        val VIRKNINGSTIDSPUNKT_JAN_23: YearMonth = YearMonth.of(2023, Month.JANUARY)
        const val TRYGDETID_40_AAR: Int = 40
        const val PRORATA_TRYGDETID_30_AAR: Int = 30
        val PRORATA_BROEK: IntBroek = IntBroek(1, 2)
        const val GRUNNBELOEP_JAN_23: Int = 9290
        const val GRUNNBELOEP_MAI_23: Int = 9885
        const val BP_BELOEP_INGEN_SOESKEN_JAN_23: Int = 3716
        const val BP_BELOEP_INGEN_SOESKEN_JAN_23_PRORATA: Int = 1394
        const val BP_BELOEP_ETT_SOESKEN_JAN_23: Int = 3019
        const val BP_BELOEP_TO_SOESKEN_JAN_23: Int = 2787
        const val BP_BELOEP_ETT_SOESKEN_MAI_23: Int = 3213
        const val BP_BELOEP_NYTT_REGELVERK_EN_DOED_FORELDER: Int = GRUNNBELOEP_MAI_23
        const val BP_BELOEP_NYTT_REGELVERK_TO_DOEDE_FORELDRE: Int = 22_241
    }
}
