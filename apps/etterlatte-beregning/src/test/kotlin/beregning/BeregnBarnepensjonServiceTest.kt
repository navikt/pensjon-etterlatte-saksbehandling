package no.nav.etterlatte.beregning

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.grunnlag.Reduksjon
import no.nav.etterlatte.beregning.grunnlag.TomVerdi
import no.nav.etterlatte.beregning.regler.MAKS_TRYGDETID
import no.nav.etterlatte.beregning.regler.barnepensjon.BP_2024_DATO
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.beregning.regler.toGrunnlag
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.klienter.TrygdetidKlient
import no.nav.etterlatte.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.Regelverk
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.AnnenForelder
import no.nav.etterlatte.libs.common.behandling.AnnenForelder.AnnenForelderVurdering.KUN_EN_REGISTRERT_JURIDISK_FORELDER
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.BeregningsMetodeBeregningsgrunnlag
import no.nav.etterlatte.libs.common.beregning.BeregningsmetodeForAvdoed
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
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
    private val anvendtTrygdetidRepository =
        mockk<AnvendtTrygdetidRepository>().also {
            every { it.lagreAnvendtTrygdetid(any(), any()) } returns 1
        }
    private val periodensSisteDato = LocalDate.of(2024, Month.APRIL, 30)

    private fun beregnBarnepensjonService() =
        BeregnBarnepensjonService(
            grunnlagKlient = grunnlagKlient,
            vilkaarsvurderingKlient = vilkaarsvurderingKlient,
            beregningsGrunnlagService = beregningsGrunnlagService,
            trygdetidKlient = trygdetidKlient,
            anvendtTrygdetidRepository = anvendtTrygdetidRepository,
        )

    @Test
    fun `skal kaste feil hvis beregningsgrunnlag hentet er på en annen behandling`() {
        val behandling = mockBehandling(BehandlingType.REVURDERING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns
            barnepensjonBeregningsGrunnlag(
                randomUUID(),
                emptyList(),
            )

        assertThrows<BeregningsgrunnlagMangler> {
            runBlocking {
                beregnBarnepensjonService().beregn(behandling, bruker)
            }
        }
    }

    @Test
    fun `skal beregne barnepensjon foerstegangsbehandling - ingen soesken - nasjonal`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns listOf(mockTrygdetid(behandling.id))

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
                    utbetaltBeloep shouldBe BP_BELOEP_INGEN_SOESKEN_JAN_23
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2023, Month.APRIL)
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_23
                    soeskenFlokk shouldBe emptyList()
                    trygdetid shouldBe MAKS_TRYGDETID
                    regelResultat shouldNotBe null
                    regelVersjon shouldNotBe null
                    regelverk shouldBe Regelverk.REGELVERK_TOM_DES_2023
                }
                with(beregningsperioder[1]) {
                    datoFOM shouldBe YearMonth.of(2023, Month.MAY)
                    utbetaltBeloep shouldBe BP_BELOEP_INGEN_SOESKEN_MAI_23
                    datoTOM shouldBe YearMonth.of(2023, Month.DECEMBER)
                }
                with(beregningsperioder[2]) {
                    datoFOM shouldBe YearMonth.of(2024, Month.JANUARY)
                    utbetaltBeloep shouldBe BP_BELOEP_NYTT_REGELVERK_EN_DOED_FORELDER
                    datoTOM shouldBe YearMonth.of(2024, Month.APRIL)
                }

                // Videre perioder gjenspeiler G-endringer
                beregningsperioder.drop(3).map { it.datoFOM } shouldContainExactly
                    GrunnbeloepRepository.historiskeGrunnbeloep
                        .filter { it.dato > YearMonth.of(2023, Month.DECEMBER) }
                        .map { it.dato }
                        .reversed()
                beregningsperioder.last().datoTOM shouldBe null

                beregningsperioder.filter { p -> BP_2024_DATO.equals(p.datoFOM) } shouldBe emptyList()
                beregningsperioder.filter { p -> p.datoTOM == null }.size shouldBe 1
            }
        }
    }

    @Test
    fun `skal beregne barnepensjon foerstegangsbehandling - ingen soesken - prorata`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList(), BeregningsMetode.PRORATA)
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns listOf(mockTrygdetid(behandling.id))

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
                    regelverk shouldBe Regelverk.REGELVERK_TOM_DES_2023
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
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList(), BeregningsMetode.BEST)
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns listOf(mockTrygdetid(behandling.id))

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
                    regelverk shouldBe Regelverk.REGELVERK_TOM_DES_2023
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
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, listOf(HELSOESKEN_FOEDSELSNUMMER))
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns listOf(mockTrygdetid(behandling.id))

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
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns
            barnepensjonBeregningsGrunnlag(
                behandling.id,
                listOf(HELSOESKEN_FOEDSELSNUMMER, HELSOESKEN2_FOEDSELSNUMMER),
            )
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns listOf(mockTrygdetid(behandling.id))

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
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns
            mockk {
                every { resultat?.utfall } returns VilkaarsvurderingUtfall.OPPFYLT
            }
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns listOf(mockTrygdetid(behandling.id))

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
    fun `Kaster feil hvis beregningsgrunnlag mangler`() {
        val behandling = mockBehandling(BehandlingType.REVURDERING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns null

        runBlocking {
            assertThrows<BeregningsgrunnlagMangler> {
                beregnBarnepensjonService().beregn(behandling, bruker)
            }
        }
    }

    @Test
    fun `skal opphoere ved revurdering og vilkaarsvurdering ikke oppfylt`() {
        val behandling = mockBehandling(BehandlingType.REVURDERING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns
            mockk {
                every { resultat?.utfall } returns VilkaarsvurderingUtfall.IKKE_OPPFYLT
            }
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns listOf(mockTrygdetid(behandling.id))

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
            beregningsGrunnlagService.hentBeregningsGrunnlag(
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
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns listOf(mockTrygdetid(behandling.id))

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
                    regelverk shouldBe Regelverk.REGELVERK_TOM_DES_2023
                }
                with(beregningsperioder.single { p -> YearMonth.of(2023, 4).equals(p.datoFOM) }) {
                    utbetaltBeloep shouldBe BP_BELOEP_ETT_SOESKEN_JAN_23
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_23
                    regelverk shouldBe Regelverk.REGELVERK_TOM_DES_2023
                }
                with(beregningsperioder.single { p -> YearMonth.of(2023, 5).equals(p.datoFOM) }) {
                    utbetaltBeloep shouldBe BP_BELOEP_ETT_SOESKEN_MAI_23
                    grunnbelopMnd shouldBe GRUNNBELOEP_MAI_23
                    regelverk shouldBe Regelverk.REGELVERK_TOM_DES_2023
                }
                with(beregningsperioder.single { p -> YearMonth.of(2024, 1).equals(p.datoFOM) }) {
                    utbetaltBeloep shouldBe BP_BELOEP_NYTT_REGELVERK_EN_DOED_FORELDER
                    grunnbelopMnd shouldBe GRUNNBELOEP_MAI_23
                    regelverk shouldBe Regelverk.REGELVERK_FOM_JAN_2024
                }
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("flereAvdoedeGrunnlag")
    fun `skal beregne barnepensjon foerstegangsbehandling - med flere avdoede foreldre og nytt regelverk`(
        beskrivelse: String,
        grunnlagFom1: LocalDate,
        grunnlagTom1: LocalDate?,
        grunnlagBeregningsMetode1: BeregningsMetode,
        grunnlagFom2: LocalDate,
        grunnlagTom2: LocalDate?,
        grunnlagBeregningsMetode2: BeregningsMetode,
        forventetUtbetalt: Int,
    ) {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns
            grunnlagMedEkstraAvdoedForelder(LocalDate.of(2023, 11, 12))
        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns
            barnepensjonBeregningsGrunnlag(
                behandling.id,
                emptyList(),
                institusjonsoppholdBeregningsgrunnlag = emptyList(),
                avdoedeBeregningmetode =
                    listOf(
                        GrunnlagMedPeriode(
                            fom = grunnlagFom1,
                            tom = grunnlagTom1,
                            data =
                                BeregningsmetodeForAvdoed(
                                    AVDOED_FOEDSELSNUMMER.value,
                                    BeregningsMetodeBeregningsgrunnlag(
                                        beregningsMetode = grunnlagBeregningsMetode1,
                                        begrunnelse = "Beskrivelse",
                                    ),
                                ),
                        ),
                        GrunnlagMedPeriode(
                            fom = grunnlagFom2,
                            tom = grunnlagTom2,
                            data =
                                BeregningsmetodeForAvdoed(
                                    AVDOED2_FOEDSELSNUMMER.value,
                                    BeregningsMetodeBeregningsgrunnlag(
                                        beregningsMetode = grunnlagBeregningsMetode2,
                                        begrunnelse = "Beskrivelse",
                                    ),
                                ),
                        ),
                    ),
            )
        coEvery {
            trygdetidKlient.hentTrygdetid(any(), any())
        } returns listOf(mockTrygdetid(behandling.id), mockTrygdetid(behandling.id, fnr = AVDOED2_FOEDSELSNUMMER.value))

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker, periodensSisteDato)
            beregning.beregningsperioder.size shouldBeGreaterThanOrEqual 3

            with(beregning.beregningsperioder[0]) {
                datoFOM shouldBe YearMonth.of(2023, 1)
                datoTOM shouldBe YearMonth.of(2023, 4)
                avdoedeForeldre shouldBe null
                kunEnJuridiskForelder shouldBe false
            }
            with(beregning.beregningsperioder[1]) {
                datoFOM shouldBe YearMonth.of(2023, 5)
                datoTOM shouldBe YearMonth.of(2023, 12)
                avdoedeForeldre shouldBe null
                kunEnJuridiskForelder shouldBe false
            }
            with(beregning.beregningsperioder[2]) {
                datoFOM shouldBe YearMonth.of(2024, 1)
                datoTOM shouldBe YearMonth.of(2024, 4)
                utbetaltBeloep shouldBe forventetUtbetalt
                avdoedeForeldre shouldBe listOf(AVDOED_FOEDSELSNUMMER.value, AVDOED2_FOEDSELSNUMMER.value)
                kunEnJuridiskForelder shouldBe false
            }
        }
    }

    @Test
    fun `beregne foerstegangsbehandling med ukjent avdoed - NYTT REGELVERK`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, virk = YearMonth.of(2024, Month.JANUARY))
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns GrunnlagTestData().hentGrunnlagMedUkjentAvdoed()

        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns
            barnepensjonBeregningsGrunnlag(
                behandling.id,
                emptyList(),
                beregningsMetode = BeregningsMetode.BEST,
                virk = YearMonth.of(2024, Month.JANUARY).atDay(1),
                avdoedeBeregningmetode =
                    listOf(
                        GrunnlagMedPeriode(
                            fom = LocalDate.of(2023, 1, 1),
                            tom = null,
                            data =
                                BeregningsmetodeForAvdoed(
                                    UKJENT_AVDOED,
                                    BeregningsMetodeBeregningsgrunnlag(
                                        beregningsMetode = BeregningsMetode.BEST,
                                        begrunnelse = "Beskrivelse",
                                    ),
                                ),
                        ),
                    ),
            )

        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns
            listOf(
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
                ),
            )

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker, periodensSisteDato)
            beregning.beregningsperioder.size shouldBeGreaterThanOrEqual 1

            with(beregning.beregningsperioder[0]) {
                datoFOM shouldBe YearMonth.of(2024, 1)
                datoTOM shouldBe YearMonth.of(2024, 4)
                utbetaltBeloep shouldBe GRUNNBELOEP_MAI_23
                kunEnJuridiskForelder shouldBe false
            }
        }
    }

    @Test
    fun `beregne foerstegangsbehandling med to avdoede foreldre, ignorere inst med NEI_KORT_OPPHOLD - NYTT REGELVERK`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, virk = YearMonth.of(2024, Month.JANUARY))
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns
            grunnlagMedEkstraAvdoedForelder(LocalDate.of(2024, 1, 12))

        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns
            barnepensjonBeregningsGrunnlag(
                behandling.id,
                emptyList(),
                institusjonsoppholdBeregningsgrunnlag = defaultInstitusjonsopphold(),
                beregningsMetode = BeregningsMetode.BEST,
                virk = YearMonth.of(2024, Month.JANUARY).atDay(1),
                avdoedeBeregningmetode =
                    listOf(
                        GrunnlagMedPeriode(
                            fom = LocalDate.of(2024, 1, 1),
                            tom = null,
                            data =
                                BeregningsmetodeForAvdoed(
                                    AVDOED_FOEDSELSNUMMER.value,
                                    BeregningsMetodeBeregningsgrunnlag(
                                        beregningsMetode = BeregningsMetode.BEST,
                                        begrunnelse = "Beskrivelse",
                                    ),
                                ),
                        ),
                        GrunnlagMedPeriode(
                            fom = LocalDate.of(2024, 1, 1),
                            tom = null,
                            data =
                                BeregningsmetodeForAvdoed(
                                    AVDOED2_FOEDSELSNUMMER.value,
                                    BeregningsMetodeBeregningsgrunnlag(
                                        beregningsMetode = BeregningsMetode.BEST,
                                        begrunnelse = "Beskrivelse",
                                    ),
                                ),
                        ),
                    ),
            )

        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns
            listOf(
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
                ),
            )
        coEvery {
            trygdetidKlient.hentTrygdetid(any(), any())
        } returns listOf(mockTrygdetid(behandling.id), mockTrygdetid(behandling.id, fnr = AVDOED2_FOEDSELSNUMMER.value))

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker, periodensSisteDato)
            beregning.beregningsperioder.size shouldBeGreaterThanOrEqual 1

            with(beregning.beregningsperioder[0]) {
                utbetaltBeloep shouldBe BP_BELOEP_NYTT_REGELVERK_TO_DOEDE_FORELDRE
            }
        }
    }

    @Test
    fun `beregne foerstegangsbehandling med flere avdoed og foreldreloes flag tar foerst trygdetid - gammelt regelverk`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns barnepensjonBeregningsGrunnlag(behandling.id, emptyList())
        coEvery {
            trygdetidKlient.hentTrygdetid(
                any(),
                any(),
            )
        } returns listOf(mockTrygdetid(behandling.id), mockTrygdetid(behandling.id, 20))

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
    fun `beregne foerstegangsbehandling med en avdoed og kun en juridisk forelder gir foreldreloessats - nytt regelverk`() {
        val virk = YearMonth.of(2024, Month.JANUARY)
        val datoAdopsjon = YearMonth.of(2024, Month.MARCH).atEndOfMonth()
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, virk = virk)
        val grunnlag =
            GrunnlagTestData(annenForelder = AnnenForelder(KUN_EN_REGISTRERT_JURIDISK_FORELDER))
                .hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns
            barnepensjonBeregningsGrunnlag(
                behandlingId = behandling.id,
                soesken = emptyList(),
                kunEnJuridiskForelder = GrunnlagMedPeriode(TomVerdi, virk.atDay(1), datoAdopsjon),
            )
        coEvery {
            trygdetidKlient.hentTrygdetid(
                any(),
                any(),
            )
        } returns listOf(mockTrygdetid(behandling.id))

        runBlocking {
            val beregning = beregnBarnepensjonService().beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 2
                with(beregningsperioder[0]) {
                    utbetaltBeloep shouldBe BP_BELOEP_NYTT_REGELVERK_TO_DOEDE_FORELDRE
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2024, Month.MARCH)
                    grunnbelopMnd shouldBe GRUNNBELOEP_MAI_23
                    soeskenFlokk shouldBe emptyList()
                    trygdetid shouldBe MAKS_TRYGDETID
                    regelResultat shouldNotBe null
                    regelVersjon shouldNotBe null
                    avdoedeForeldre shouldBe listOf(AVDOED_FOEDSELSNUMMER.value)
                    kunEnJuridiskForelder shouldBe true
                }
                with(beregningsperioder[1]) {
                    utbetaltBeloep shouldBe BP_BELOEP_NYTT_REGELVERK_EN_DOED_FORELDER
                    datoFOM shouldBe YearMonth.of(2024, Month.APRIL)
                    datoTOM shouldBe YearMonth.of(2024, Month.APRIL)
                    grunnbelopMnd shouldBe GRUNNBELOEP_MAI_23
                    soeskenFlokk shouldBe emptyList()
                    trygdetid shouldBe MAKS_TRYGDETID
                    regelResultat shouldNotBe null
                    regelVersjon shouldNotBe null
                    avdoedeForeldre shouldBe listOf(AVDOED_FOEDSELSNUMMER.value)
                    kunEnJuridiskForelder shouldBe false
                }
                beregningsperioder.filter { p -> BP_2024_DATO.equals(p.datoFOM) } shouldBe emptyList()
            }
        }
    }

    @Test
    fun `skal ikke tillate kun en juridisk forelder hvis ikke registrert i persongalleri`() {
        val virk = YearMonth.of(2024, Month.JANUARY)
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, virk = virk)

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns
            barnepensjonBeregningsGrunnlag(
                behandlingId = behandling.id,
                soesken = emptyList(),
                kunEnJuridiskForelder =
                    GrunnlagMedPeriode(
                        TomVerdi,
                        virk.atDay(1),
                        LocalDate.of(2025, 6, 1),
                    ),
            )
        assertThrows<BPBeregningsgrunnlagKunEnJuridiskForelderFinnesIkkeIPersongalleri> {
            runBlocking {
                beregnBarnepensjonService().beregn(behandling, bruker)
            }
        }
    }

    @Test
    fun `skal ikke tillate kun en juridisk forelder med startdato forskjellig fra virk`() {
        val virk = YearMonth.of(2024, Month.JANUARY)
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, virk = virk)

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns
            GrunnlagTestData(
                annenForelder = AnnenForelder(KUN_EN_REGISTRERT_JURIDISK_FORELDER),
            ).hentOpplysningsgrunnlag()
        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns
            barnepensjonBeregningsGrunnlag(
                behandlingId = behandling.id,
                soesken = emptyList(),
                kunEnJuridiskForelder =
                    GrunnlagMedPeriode(
                        TomVerdi,
                        virk.plusMonths(2).atDay(1),
                        LocalDate.of(2025, 6, 1),
                    ),
            )
        assertThrows<BPKunEnJuridiskForelderMaaGjeldeFraVirkningstidspunkt> {
            runBlocking {
                beregnBarnepensjonService().beregn(behandling, bruker)
            }
        }
    }

    private fun grunnlagMedEkstraAvdoedForelder(doedsdato: LocalDate): Grunnlag {
        val grunnlagEnAvdoed = GrunnlagTestData().hentOpplysningsgrunnlag()
        val nyligAvdoedFoedselsnummer = AVDOED2_FOEDSELSNUMMER
        val nyligAvdoed: Grunnlagsdata<JsonNode> =
            mapOf(
                Opplysningstype.DOEDSDATO to konstantOpplysning(doedsdato),
                Opplysningstype.PERSONROLLE to konstantOpplysning(AVDOED),
                Opplysningstype.FOEDSELSNUMMER to konstantOpplysning(nyligAvdoedFoedselsnummer),
            )
        return GrunnlagTestData(
            opplysningsmapAvdoedeOverrides = listOf(nyligAvdoed) + grunnlagEnAvdoed.hentAvdoede(),
        ).hentOpplysningsgrunnlag()
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
        virk: LocalDate = VIRKNINGSTIDSPUNKT_JAN_2023.minusMonths(1).atDay(1),
        avdoedeBeregningmetode: List<GrunnlagMedPeriode<BeregningsmetodeForAvdoed>> =
            defaultAvdoedeBeregningmetode(
                beregningsMetode,
            ),
        kunEnJuridiskForelder: GrunnlagMedPeriode<TomVerdi>? = null,
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
        institusjonsopphold = institusjonsoppholdBeregningsgrunnlag,
        beregningsMetode = beregningsMetode.toGrunnlag(),
        beregningsMetodeFlereAvdoede = avdoedeBeregningmetode,
        kunEnJuridiskForelder = kunEnJuridiskForelder,
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
        institusjonsopphold = defaultInstitusjonsopphold(),
        beregningsMetode = BeregningsMetode.NASJONAL.toGrunnlag(),
        beregningsMetodeFlereAvdoede = defaultAvdoedeBeregningmetode(),
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

    private fun defaultAvdoedeBeregningmetode(beregningsMetode: BeregningsMetode = BeregningsMetode.BEST) =
        listOf(
            GrunnlagMedPeriode(
                fom = LocalDate.of(2023, 1, 1),
                tom = null,
                data =
                    BeregningsmetodeForAvdoed(
                        AVDOED_FOEDSELSNUMMER.value,
                        BeregningsMetodeBeregningsgrunnlag(
                            beregningsMetode = beregningsMetode,
                            begrunnelse = "Beskrivelse",
                        ),
                    ),
            ),
        )

    private fun mockBehandling(
        type: BehandlingType,
        virk: YearMonth = VIRKNINGSTIDSPUNKT_JAN_2023,
        vedtaksloesning: Vedtaksloesning = Vedtaksloesning.GJENNY,
    ): DetaljertBehandling =
        mockk<DetaljertBehandling> {
            every { id } returns randomUUID()
            every { sak } returns sakId1
            every { behandlingType } returns type
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt(virk)
            every { kilde } returns vedtaksloesning
            every { revurderingsaarsak } returns null
        }

    private fun mockTrygdetid(
        behandlingId_: UUID,
        anvendtTrygdetid: Int = TRYGDETID_40_AAR,
        fnr: String = AVDOED_FOEDSELSNUMMER.value,
    ): TrygdetidDto =
        mockk<TrygdetidDto>().apply {
            every { id } returns randomUUID()
            every { behandlingId } returns behandlingId_
            every { ident } returns fnr
            every { beregnetTrygdetid } returns
                mockk {
                    every { resultat } returns
                        mockk {
                            every { samletTrygdetidNorge } returns anvendtTrygdetid
                            every { samletTrygdetidTeoretisk } returns PRORATA_TRYGDETID_30_AAR
                            every { prorataBroek } returns PRORATA_BROEK
                        }
                    every { tidspunkt } returns Tidspunkt.now()
                }
        }

    companion object {
        val VIRKNINGSTIDSPUNKT_JAN_2023: YearMonth = YearMonth.of(2023, Month.JANUARY)
        const val TRYGDETID_40_AAR: Int = 40
        const val PRORATA_TRYGDETID_30_AAR: Int = 30
        val PRORATA_BROEK: IntBroek = IntBroek(1, 2)
        const val GRUNNBELOEP_JAN_23: Int = 9290
        const val GRUNNBELOEP_MAI_23: Int = 9885
        const val GRUNNBELOEP_MAI_24: Int = 10336
        const val BP_BELOEP_INGEN_SOESKEN_JAN_23: Int = 3716
        const val BP_BELOEP_INGEN_SOESKEN_MAI_23: Int = 3954
        const val BP_BELOEP_INGEN_SOESKEN_JAN_23_PRORATA: Int = 1394
        const val BP_BELOEP_ETT_SOESKEN_JAN_23: Int = 3019
        const val BP_BELOEP_TO_SOESKEN_JAN_23: Int = 2787
        const val BP_BELOEP_ETT_SOESKEN_MAI_23: Int = 3213
        const val BP_BELOEP_NYTT_REGELVERK_EN_DOED_FORELDER: Int = GRUNNBELOEP_MAI_23
        const val BP_BELOEP_NYTT_REGELVERK_TO_DOEDE_FORELDRE: Int = 22_241

        @JvmStatic
        fun flereAvdoedeGrunnlag() =
            listOf(
                Arguments.of(
                    "Begge har BEST",
                    LocalDate.of(2023, 1, 1),
                    null,
                    BeregningsMetode.BEST,
                    LocalDate.of(2023, 1, 1),
                    null,
                    BeregningsMetode.BEST,
                    BP_BELOEP_NYTT_REGELVERK_TO_DOEDE_FORELDRE,
                ),
                Arguments.of(
                    "Begge har NASJONAL",
                    LocalDate.of(2023, 1, 1),
                    null,
                    BeregningsMetode.NASJONAL,
                    LocalDate.of(2023, 1, 1),
                    null,
                    BeregningsMetode.NASJONAL,
                    BP_BELOEP_NYTT_REGELVERK_TO_DOEDE_FORELDRE,
                ),
                Arguments.of(
                    "Begge har PRORATA",
                    LocalDate.of(2023, 1, 1),
                    null,
                    BeregningsMetode.PRORATA,
                    LocalDate.of(2023, 1, 1),
                    null,
                    BeregningsMetode.PRORATA,
                    8340,
                ),
                Arguments.of(
                    "En har PRORATA, en har BEST",
                    LocalDate.of(2023, 1, 1),
                    null,
                    BeregningsMetode.BEST,
                    LocalDate.of(2023, 1, 1),
                    null,
                    BeregningsMetode.PRORATA,
                    BP_BELOEP_NYTT_REGELVERK_TO_DOEDE_FORELDRE,
                ),
                Arguments.of(
                    "Begge har BEST men en har bare begrenset tid",
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 4, 30),
                    BeregningsMetode.BEST,
                    LocalDate.of(2023, 1, 1),
                    null,
                    BeregningsMetode.PRORATA,
                    BP_BELOEP_NYTT_REGELVERK_TO_DOEDE_FORELDRE,
                ),
            )
    }
}
