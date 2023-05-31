package no.nav.etterlatte.beregning

import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.klienter.TrygdetidKlient
import no.nav.etterlatte.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Month
import java.time.YearMonth
import java.util.*
import java.util.UUID.randomUUID

internal class BeregnOmstillingsstoenadServiceTest {

    private val vilkaarsvurderingKlient = mockk<VilkaarsvurderingKlient>()
    private val grunnlagKlient = mockk<GrunnlagKlientImpl>()
    private val trygdetidKlient = mockk<TrygdetidKlient>()
    private val beregnOmstillingsstoenadService = BeregnOmstillingsstoenadService(
        grunnlagKlient = grunnlagKlient,
        vilkaarsvurderingKlient = vilkaarsvurderingKlient,
        trygdetidKlient = trygdetidKlient
    )

    @Test
    fun `skal beregne omstillingsstoenad foerstegangsbehandling`() {
        val behandling = mockBehandling(BehandlingType.FØRSTEGANGSBEHANDLING)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val trygdetid = mockTrygdetid(behandling.id)

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns trygdetid

        runBlocking {
            val beregning = beregnOmstillingsstoenadService.beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.OMS
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 2
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe OMS_BELOEP_JAN_23
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2023, Month.APRIL)
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_23
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
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns mockk {
            every { resultat?.utfall } returns VilkaarsvurderingUtfall.OPPFYLT
        }
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns trygdetid

        runBlocking {
            val beregning = beregnOmstillingsstoenadService.beregn(behandling, bruker)

            with(beregning) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.OMS
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 2
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe OMS_BELOEP_JAN_23
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2023, Month.APRIL)
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_23
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
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns mockk {
            every { resultat?.utfall } returns VilkaarsvurderingUtfall.IKKE_OPPFYLT
        }
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns trygdetid

        runBlocking {
            val beregning = beregnOmstillingsstoenadService.beregn(behandling, bruker)

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
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_23
                    this.trygdetid shouldBe 0
                }
            }
        }
    }

    @Test
    fun `skal sette beloep til 0 ved manuelt opphoer`() {
        val behandling = mockBehandling(BehandlingType.MANUELT_OPPHOER)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val trygdetid = mockTrygdetid(behandling.id)

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns trygdetid

        runBlocking {
            val beregning = beregnOmstillingsstoenadService.beregn(behandling, bruker)

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
                    grunnbelopMnd shouldBe GRUNNBELOEP_JAN_23
                    soeskenFlokk shouldBe null
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
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns trygdetid

        runBlocking {
            assertThrows<IllegalArgumentException> {
                beregnOmstillingsstoenadService.beregn(behandling, bruker)
            }
        }
    }

    private fun mockBehandling(type: BehandlingType, virk: YearMonth = VIRKNINGSTIDSPUNKT_JAN_23): DetaljertBehandling =
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
                every { total } returns TRYGDETID_40_AAR
                every { tidspunkt } returns Tidspunkt.now()
            }
        }

    private fun mockTrygdetidUtenBeregnetTrygdetid(behandlingId_: UUID): TrygdetidDto =
        mockk<TrygdetidDto>().apply {
            every { id } returns randomUUID()
            every { behandlingId } returns behandlingId_
            every { beregnetTrygdetid } returns null
        }

    companion object {
        val VIRKNINGSTIDSPUNKT_JAN_23: YearMonth = YearMonth.of(2023, 1)
        const val TRYGDETID_40_AAR: Int = 40
        const val GRUNNBELOEP_JAN_23: Int = 9290
        const val OMS_BELOEP_JAN_23: Int = 20902
    }
}