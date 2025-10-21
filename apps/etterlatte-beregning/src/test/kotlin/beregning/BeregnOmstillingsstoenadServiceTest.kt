package no.nav.etterlatte.beregning

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
import no.nav.etterlatte.beregning.regler.STANDARDSAK
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.beregning.regler.toGrunnlag
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.klienter.TrygdetidKlient
import no.nav.etterlatte.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.Regelverk
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.beregning.Sanksjon
import no.nav.etterlatte.libs.common.beregning.SanksjonType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.sanksjon.SanksjonService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import java.util.UUID.randomUUID

internal class BeregnOmstillingsstoenadServiceTest {
    private val vilkaarsvurderingKlient = mockk<VilkaarsvurderingKlient>()
    private val grunnlagKlient = mockk<GrunnlagKlientImpl>()
    private val trygdetidKlient = mockk<TrygdetidKlient>()
    private val beregningsGrunnlagService = mockk<BeregningsGrunnlagService>()
    private val sanksjonService = mockk<SanksjonService>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private lateinit var beregnOmstillingsstoenadService: BeregnOmstillingsstoenadService

    private val periodensSisteDato = LocalDate.of(2024, Month.APRIL, 30)

    @BeforeEach
    fun setup() {
        beregnOmstillingsstoenadService =
            BeregnOmstillingsstoenadService(
                grunnlagKlient = grunnlagKlient,
                vilkaarsvurderingKlient = vilkaarsvurderingKlient,
                trygdetidKlient = trygdetidKlient,
                beregningsGrunnlagService = beregningsGrunnlagService,
                featureToggleService = featureToggleService,
                sanksjonService = sanksjonService,
            )

        every { featureToggleService.isEnabled(any(), any()) } returns false
        every { sanksjonService.hentSanksjon(any()) } returns emptyList()
    }

    @Test
    fun `skal kaste feil hvis beregningsgrunnlag hentet er på en annen behandling`() {
        val behandling = mockBehandling(BehandlingType.REVURDERING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val trygdetid = mockTrygdetid(behandling.id)

        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns listOf(trygdetid)
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns
            omstillingstoenadBeregningsGrunnlag(
                randomUUID(),
            )
        assertThrows<BeregningsgrunnlagMangler> {
            runBlocking {
                beregnOmstillingsstoenadService.beregn(behandling, bruker)
            }
        }
    }

    @Test
    fun `skal beregne omstillingsstoenad foerstegangsbehandling - nasjonal`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val trygdetid = mockTrygdetid(behandling.id)

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns listOf(trygdetid)
        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns omstillingstoenadBeregningsGrunnlag(behandling.id)

        runBlocking {
            val beregning = beregnOmstillingsstoenadService.beregn(behandling, bruker, periodensSisteDato)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.OMS
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBe 1
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe OMS_BELOEP_JAN_24
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2024, Month.APRIL)
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_24
                    soeskenFlokk shouldBe null
                    this.trygdetid shouldBe TRYGDETID_40_AAR
                    regelResultat shouldNotBe null
                    regelVersjon shouldNotBe null
                    regelverk shouldBe Regelverk.REGELVERK_FOM_JAN_2024
                }
            }
        }
    }

    @Test
    fun `skal beregne omstillingsstoenad foerstegangsbehandling - sanksjon`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val trygdetid = mockTrygdetid(behandling.id)
        val sanksjon =
            sanksjon(behandlingId = behandling.id, fom = YearMonth.of(2024, Month.FEBRUARY), tom = YearMonth.of(2024, Month.MARCH))

        every { featureToggleService.isEnabled(any(), any()) } returns true
        every { sanksjonService.hentSanksjon(any()) } returns listOf(sanksjon)
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns listOf(trygdetid)
        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns omstillingstoenadBeregningsGrunnlag(behandling.id)

        runBlocking {
            val beregning = beregnOmstillingsstoenadService.beregn(behandling, bruker, periodensSisteDato)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.OMS
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBe 3
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe OMS_BELOEP_JAN_24
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2024, Month.JANUARY)
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_24
                    soeskenFlokk shouldBe null
                    this.trygdetid shouldBe TRYGDETID_40_AAR
                    regelResultat shouldNotBe null
                    regelVersjon shouldNotBe null
                    regelverk shouldBe Regelverk.REGELVERK_FOM_JAN_2024
                }
                with(beregningsperioder[1]) {
                    utbetaltBeloep shouldBe OMS_BELOEP_SANKSJON
                    datoFOM shouldBe YearMonth.of(2024, Month.FEBRUARY)
                    datoTOM shouldBe YearMonth.of(2024, Month.MARCH)
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_24
                    soeskenFlokk shouldBe null
                    this.trygdetid shouldBe TRYGDETID_40_AAR
                    regelResultat shouldNotBe null
                    regelVersjon shouldNotBe null
                    regelverk shouldBe Regelverk.REGELVERK_FOM_JAN_2024
                }
                with(beregningsperioder[2]) {
                    utbetaltBeloep shouldBe OMS_BELOEP_JAN_24
                    datoFOM shouldBe YearMonth.of(2024, Month.APRIL)
                    datoTOM shouldBe YearMonth.of(2024, Month.APRIL)
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_24
                    soeskenFlokk shouldBe null
                    this.trygdetid shouldBe TRYGDETID_40_AAR
                    regelResultat shouldNotBe null
                    regelVersjon shouldNotBe null
                    regelverk shouldBe Regelverk.REGELVERK_FOM_JAN_2024
                }
            }
        }
    }

    @Test
    fun `skal beregne omstillingsstoenad foerstegangsbehandling - prorata`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val trygdetid = mockTrygdetid(behandling.id)

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns listOf(trygdetid)
        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns omstillingstoenadBeregningsGrunnlag(behandling.id, BeregningsMetode.PRORATA)

        runBlocking {
            val beregning = beregnOmstillingsstoenadService.beregn(behandling, bruker, periodensSisteDato)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.OMS
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBe 1
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe OMS_BELOEP_JAN_24_PRORATA
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2024, Month.APRIL)
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_24
                    soeskenFlokk shouldBe null
                    this.trygdetid shouldBe PRORATA_TRYGDETID_30_AAR / 2
                    regelResultat shouldNotBe null
                    regelVersjon shouldNotBe null
                }
            }
        }
    }

    @Test
    fun `skal beregne omstillingsstoenad foerstegangsbehandling - best`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val trygdetid = mockTrygdetid(behandling.id)

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns listOf(trygdetid)
        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns omstillingstoenadBeregningsGrunnlag(behandling.id, BeregningsMetode.BEST)

        runBlocking {
            val beregning = beregnOmstillingsstoenadService.beregn(behandling, bruker, periodensSisteDato)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.OMS
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBe 1
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe OMS_BELOEP_JAN_24
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2024, Month.APRIL)
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_24
                    soeskenFlokk shouldBe null
                    this.trygdetid shouldBe TRYGDETID_40_AAR
                    regelResultat shouldNotBe null
                    regelVersjon shouldNotBe null
                }
            }
        }
    }

    @Test
    fun `skal beregne omstillingsstoenad ved revurdering`() {
        val behandling = mockBehandling(BehandlingType.REVURDERING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val trygdetid = mockTrygdetid(behandling.id)

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns
            mockk {
                every { resultat?.utfall } returns VilkaarsvurderingUtfall.OPPFYLT
            }
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns listOf(trygdetid)

        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns omstillingstoenadBeregningsGrunnlag(behandling.id)

        runBlocking {
            val beregning = beregnOmstillingsstoenadService.beregn(behandling, bruker, periodensSisteDato)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.OMS
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBe 1
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe OMS_BELOEP_JAN_24
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2024, Month.APRIL)
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_24
                    this.trygdetid shouldBe TRYGDETID_40_AAR
                }
            }
        }
    }

    @Test
    fun `skal opphoere ved revurdering og vilkaarsvurdering ikke oppfylt`() {
        val behandling = mockBehandling(BehandlingType.REVURDERING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val trygdetid = mockTrygdetid(behandling.id)

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns
            mockk {
                every { resultat?.utfall } returns VilkaarsvurderingUtfall.IKKE_OPPFYLT
            }
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns listOf(trygdetid)
        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns omstillingstoenadBeregningsGrunnlag(behandling.id)

        runBlocking {
            val beregning = beregnOmstillingsstoenadService.beregn(behandling, bruker, periodensSisteDato)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.OMS
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBe 1
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe 0
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe null
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_24
                    this.trygdetid shouldBe 0
                }
            }
        }
    }

    @Test
    fun `skal feile hvis trygdetid ikke inneholder beregnet trygdetid`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val trygdetid = mockTrygdetidUtenBeregnetTrygdetid(behandling.id)

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns listOf(trygdetid)
        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns omstillingstoenadBeregningsGrunnlag(behandling.id)

        runBlocking {
            assertThrows<TrygdetidMangler> {
                beregnOmstillingsstoenadService.beregn(behandling, bruker, periodensSisteDato)
            }
        }
    }

    @Test
    fun `skal feile hvis trygdetid ikke finnes`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns emptyList()
        coEvery {
            beregningsGrunnlagService.hentBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns omstillingstoenadBeregningsGrunnlag(behandling.id)

        runBlocking {
            assertThrows<TrygdetidMangler> {
                beregnOmstillingsstoenadService.beregn(behandling, bruker, periodensSisteDato)
            }
        }
    }

    private fun mockBehandling(
        type: BehandlingType,
        virk: YearMonth = VIRKNINGSTIDSPUNKT_JAN_24,
    ): DetaljertBehandling =
        mockk<DetaljertBehandling>().apply {
            every { id } returns randomUUID()
            every { sak } returns sakId1
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
                            every { samletTrygdetidNorge } returns TRYGDETID_40_AAR
                            every { samletTrygdetidTeoretisk } returns PRORATA_TRYGDETID_30_AAR
                            every { prorataBroek } returns PRORATA_BROEK
                        }
                    every { tidspunkt } returns Tidspunkt.now()
                }
            every { ident } returns AVDOED_FOEDSELSNUMMER.value
        }

    private fun mockTrygdetidUtenBeregnetTrygdetid(behandlingId_: UUID): TrygdetidDto =
        mockk<TrygdetidDto>().apply {
            every { id } returns randomUUID()
            every { behandlingId } returns behandlingId_
            every { beregnetTrygdetid } returns null
        }

    fun sanksjon(
        id: UUID? = randomUUID(),
        behandlingId: UUID,
        sakId: SakId = STANDARDSAK,
        fom: YearMonth,
        tom: YearMonth,
        type: SanksjonType = SanksjonType.STANS,
        beskrivelse: String = "Ikke i jobb",
    ) = Sanksjon(
        id = id,
        behandlingId = behandlingId,
        sakId = sakId,
        type = type,
        fom = fom,
        tom = tom,
        opprettet = Grunnlagsopplysning.Saksbehandler.create("A12345"),
        endret = Grunnlagsopplysning.Saksbehandler.create("A12345"),
        beskrivelse = beskrivelse,
    )

    companion object {
        val VIRKNINGSTIDSPUNKT_JAN_24: YearMonth = YearMonth.of(2024, 1)
        const val TRYGDETID_40_AAR: Int = 40
        const val PRORATA_TRYGDETID_30_AAR: Int = 30
        val PRORATA_BROEK: IntBroek = IntBroek(1, 2)
        const val GRUNNBELOEP_JAN_24: Int = 9885
        const val OMS_BELOEP_JAN_24: Int = 22241
        const val OMS_BELOEP_SANKSJON: Int = 0
        const val OMS_BELOEP_JAN_24_PRORATA: Int = 8340
    }

    private fun omstillingstoenadBeregningsGrunnlag(
        behandlingId: UUID,
        beregningsMetode: BeregningsMetode = BeregningsMetode.NASJONAL,
    ) = BeregningsGrunnlag(
        behandlingId,
        mockk {
            every { ident } returns "Z123456"
            every { tidspunkt } returns Tidspunkt.now()
            every { type } returns ""
        },
        institusjonsopphold =
            listOf(
                GrunnlagMedPeriode(
                    fom = LocalDate.of(2022, 8, 1),
                    tom = null,
                    data = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.NEI_KORT_OPPHOLD),
                ),
            ),
        beregningsMetode = beregningsMetode.toGrunnlag(),
    )
}
