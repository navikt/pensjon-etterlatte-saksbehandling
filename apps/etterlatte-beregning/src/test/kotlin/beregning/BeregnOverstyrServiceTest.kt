package no.nav.etterlatte.beregning

import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlag
import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlagData
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.beregning.OverstyrtBeregningKategori
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

internal class BeregnOverstyrServiceTest {
    private val grunnlagKlient = mockk<GrunnlagKlientImpl>()
    private val beregningsGrunnlagService = mockk<BeregningsGrunnlagService>()
    private val vilkaarsvurderingKlient = mockk<VilkaarsvurderingKlient>()
    private lateinit var beregnOverstyrBeregningService: BeregnOverstyrBeregningService

    @BeforeEach
    fun setup() {
        beregnOverstyrBeregningService =
            BeregnOverstyrBeregningService(
                grunnlagKlient = grunnlagKlient,
                beregningsGrunnlagService = beregningsGrunnlagService,
                vilkaarsvurderingKlient = vilkaarsvurderingKlient,
            )
    }

    @Test
    fun `skal beregne overstyrt foerstegangsbehandling`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, YearMonth.of(2019, 11))
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        every { beregningsGrunnlagService.hentOverstyrBeregningGrunnlag(any()) } returns
            OverstyrBeregningGrunnlag(
                perioder =
                    listOf(
                        GrunnlagMedPeriode(
                            OverstyrBeregningGrunnlagData(
                                utbetaltBeloep = 123L,
                                trygdetid = 20L,
                                trygdetidForIdent = null,
                                prorataBroekTeller = null,
                                prorataBroekNevner = null,
                                beskrivelse = "test periode 1",
                                aarsak = "ANNET",
                            ),
                            LocalDate.of(2019, 11, 1),
                            LocalDate.of(2020, 4, 30),
                        ),
                        GrunnlagMedPeriode(
                            OverstyrBeregningGrunnlagData(
                                utbetaltBeloep = 456,
                                trygdetid = 10L,
                                trygdetidForIdent = null,
                                prorataBroekTeller = null,
                                prorataBroekNevner = null,
                                beskrivelse = "test periode 2",
                                aarsak = "ANNET",
                            ),
                            LocalDate.of(2020, 5, 1),
                            null,
                        ),
                    ),
                kilde =
                    mockk {
                        every { ident } returns "Z123456"
                        every { tidspunkt } returns Tidspunkt.now()
                        every { type } returns ""
                    },
            )

        runBlocking {
            val beregning =
                beregnOverstyrBeregningService.beregn(
                    behandling,
                    OverstyrBeregning(behandling.sak, "Test", Tidspunkt.now(), kategori = OverstyrtBeregningKategori.FORELDRELOS),
                    bruker,
                )

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.OMS
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 2
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe 123
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2020, Month.APRIL)
                    grunnbelop shouldBe 99858
                    grunnbelopMnd shouldBe 8322
                    soeskenFlokk shouldBe null
                    institusjonsopphold shouldBe null
                    trygdetid shouldBe 20
                    samletNorskTrygdetid shouldBe 20
                    samletTeoretiskTrygdetid shouldBe null
                    broek shouldBe null
                    regelResultat shouldNotBe null
                    regelVersjon shouldNotBe null
                }
            }
        }
    }

    @Test
    fun `skal beregne overstyrt foerstegangsbehandling med prorata`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, YearMonth.of(2019, 11))
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        every { beregningsGrunnlagService.hentOverstyrBeregningGrunnlag(any()) } returns
            OverstyrBeregningGrunnlag(
                perioder =
                    listOf(
                        GrunnlagMedPeriode(
                            OverstyrBeregningGrunnlagData(
                                utbetaltBeloep = 123L,
                                trygdetid = 20L,
                                trygdetidForIdent = null,
                                prorataBroekTeller = 10,
                                prorataBroekNevner = 20,
                                beskrivelse = "test periode 1",
                                aarsak = "ANNET",
                            ),
                            LocalDate.of(2019, 11, 1),
                            LocalDate.of(2020, 4, 30),
                        ),
                        GrunnlagMedPeriode(
                            OverstyrBeregningGrunnlagData(
                                utbetaltBeloep = 456,
                                trygdetid = 10L,
                                trygdetidForIdent = null,
                                prorataBroekTeller = 10,
                                prorataBroekNevner = 20,
                                beskrivelse = "test periode 2",
                                aarsak = "ANNET",
                            ),
                            LocalDate.of(2020, 5, 1),
                            null,
                        ),
                    ),
                kilde =
                    mockk {
                        every { ident } returns "Z123456"
                        every { tidspunkt } returns Tidspunkt.now()
                        every { type } returns ""
                    },
            )

        runBlocking {
            val beregning =
                beregnOverstyrBeregningService.beregn(
                    behandling,
                    OverstyrBeregning(behandling.sak, "Test", Tidspunkt.now(), kategori = OverstyrtBeregningKategori.FORELDRELOS),
                    bruker,
                )

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.OMS
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 2
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe 123
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2020, Month.APRIL)
                    grunnbelop shouldBe 99858
                    grunnbelopMnd shouldBe 8322
                    soeskenFlokk shouldBe null
                    institusjonsopphold shouldBe null
                    trygdetid shouldBe 10
                    samletNorskTrygdetid shouldBe null
                    samletTeoretiskTrygdetid shouldBe 20
                    broek shouldBe IntBroek(10, 20)
                    regelResultat shouldNotBe null
                    regelVersjon shouldNotBe null
                }
            }
        }
    }

    @Test
    fun `skal beregne overstyrt revurdering hvis vilkaar er oppfylt`() {
        val behandling = mockBehandling(BehandlingType.REVURDERING, YearMonth.of(2019, 11))
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns vilkaarsvurderingDto
        every { vilkaarsvurderingDto.resultat } returns
            mockk {
                every { utfall } returns VilkaarsvurderingUtfall.OPPFYLT
            }
        every { beregningsGrunnlagService.hentOverstyrBeregningGrunnlag(any()) } returns
            OverstyrBeregningGrunnlag(
                perioder =
                    listOf(
                        GrunnlagMedPeriode(
                            OverstyrBeregningGrunnlagData(
                                utbetaltBeloep = 123L,
                                trygdetid = 20L,
                                trygdetidForIdent = null,
                                prorataBroekTeller = null,
                                prorataBroekNevner = null,
                                beskrivelse = "test periode 1",
                                aarsak = "ANNET",
                            ),
                            LocalDate.of(2019, 11, 1),
                            LocalDate.of(2020, 4, 30),
                        ),
                        GrunnlagMedPeriode(
                            OverstyrBeregningGrunnlagData(
                                utbetaltBeloep = 456,
                                trygdetid = 10L,
                                trygdetidForIdent = null,
                                prorataBroekTeller = null,
                                prorataBroekNevner = null,
                                beskrivelse = "test periode 2",
                                aarsak = "ANNET",
                            ),
                            LocalDate.of(2020, 5, 1),
                            null,
                        ),
                    ),
                kilde =
                    mockk {
                        every { ident } returns "Z123456"
                        every { tidspunkt } returns Tidspunkt.now()
                        every { type } returns ""
                    },
            )

        runBlocking {
            val beregning =
                beregnOverstyrBeregningService.beregn(
                    behandling,
                    OverstyrBeregning(behandling.sak, "Test", Tidspunkt.now(), kategori = OverstyrtBeregningKategori.FORELDRELOS),
                    bruker,
                )

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.OMS
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 2
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe 123
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2020, Month.APRIL)
                    grunnbelop shouldBe 99858
                    grunnbelopMnd shouldBe 8322
                    soeskenFlokk shouldBe null
                    institusjonsopphold shouldBe null
                    trygdetid shouldBe 20
                    samletNorskTrygdetid shouldBe 20
                    samletTeoretiskTrygdetid shouldBe null
                    broek shouldBe null
                    regelResultat shouldNotBe null
                    regelVersjon shouldNotBe null
                }
            }
        }
    }

    @Test
    fun `skal beregne overstyrt revurdering hvis vilkaar som opphoer ikke er oppfylt`() {
        val behandling = mockBehandling(BehandlingType.REVURDERING, YearMonth.of(2019, 11))
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns vilkaarsvurderingDto
        every { vilkaarsvurderingDto.resultat } returns
            mockk {
                every { utfall } returns VilkaarsvurderingUtfall.IKKE_OPPFYLT
            }
        every { beregningsGrunnlagService.hentOverstyrBeregningGrunnlag(any()) } returns
            OverstyrBeregningGrunnlag(
                perioder =
                    listOf(
                        GrunnlagMedPeriode(
                            OverstyrBeregningGrunnlagData(
                                utbetaltBeloep = 123L,
                                trygdetid = 20L,
                                trygdetidForIdent = null,
                                prorataBroekTeller = null,
                                prorataBroekNevner = null,
                                beskrivelse = "test periode 1",
                                aarsak = "ANNET",
                            ),
                            LocalDate.of(2019, 11, 1),
                            LocalDate.of(2020, 4, 30),
                        ),
                        GrunnlagMedPeriode(
                            OverstyrBeregningGrunnlagData(
                                utbetaltBeloep = 456,
                                trygdetid = 10L,
                                trygdetidForIdent = null,
                                prorataBroekTeller = null,
                                prorataBroekNevner = null,
                                beskrivelse = "test periode 2",
                                aarsak = "ANNET",
                            ),
                            LocalDate.of(2020, 5, 1),
                            null,
                        ),
                    ),
                kilde =
                    mockk {
                        every { ident } returns "Z123456"
                        every { tidspunkt } returns Tidspunkt.now()
                        every { type } returns ""
                    },
            )

        runBlocking {
            val beregning =
                beregnOverstyrBeregningService.beregn(
                    behandling,
                    OverstyrBeregning(behandling.sak, "Test", Tidspunkt.now(), kategori = OverstyrtBeregningKategori.FORELDRELOS),
                    bruker,
                )

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.OMS
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 2
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe 0
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2020, Month.APRIL)
                    grunnbelop shouldBe 99858
                    grunnbelopMnd shouldBe 8322
                    soeskenFlokk shouldBe null
                    institusjonsopphold shouldBe null
                    trygdetid shouldBe 0
                    samletNorskTrygdetid shouldBe 0
                    samletTeoretiskTrygdetid shouldBe null
                    broek shouldBe null
                    regelResultat shouldNotBe null
                    regelVersjon shouldNotBe null
                }
            }
        }
    }

    private fun mockBehandling(
        type: BehandlingType,
        virk: YearMonth,
        saksType: SakType = SakType.OMSTILLINGSSTOENAD,
    ): DetaljertBehandling =
        mockk<DetaljertBehandling>().apply {
            every { id } returns UUID.randomUUID()
            every { sak } returns 1
            every { behandlingType } returns type
            every { sakType } returns saksType
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt(virk)
        }
}
