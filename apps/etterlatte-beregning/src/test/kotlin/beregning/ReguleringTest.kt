package no.nav.etterlatte.beregning.regler.beregning

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.BeregnBarnepensjonService
import no.nav.etterlatte.beregning.BeregnBarnepensjonServiceFeatureToggle
import no.nav.etterlatte.beregning.BeregnBarnepensjonServiceTest
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.grunnlag.Reduksjon
import no.nav.etterlatte.beregning.regler.MAKS_TRYGDETID
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository.hentGjeldendeGrunnbeloep
import no.nav.etterlatte.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.klienter.TrygdetidKlient
import no.nav.etterlatte.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import kotlin.math.abs

class ReguleringTest {
    private val vilkaarsvurderingKlient = mockk<VilkaarsvurderingKlient>()
    private val grunnlagKlient = mockk<GrunnlagKlientImpl>()
    private val beregningsGrunnlagService = mockk<BeregningsGrunnlagService>()
    private val trygdetidKlient = mockk<TrygdetidKlient>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private lateinit var beregnBarnepensjonService: BeregnBarnepensjonService

    @BeforeEach
    fun setup() {
        every {
            featureToggleService.isEnabled(BeregnBarnepensjonServiceFeatureToggle.BrukNyttRegelverkIBeregning, false)
        } returns true
        beregnBarnepensjonService =
            BeregnBarnepensjonService(
                grunnlagKlient = grunnlagKlient,
                vilkaarsvurderingKlient = vilkaarsvurderingKlient,
                beregningsGrunnlagService = beregningsGrunnlagService,
                trygdetidKlient = trygdetidKlient,
                featureToggleService = featureToggleService,
            )
    }

    @Test
    fun `skal regulere barnepensjon foerstegangsbehandling - ingen soesken`() {
        val virk = BeregnBarnepensjonServiceTest.VIRKNINGSTIDSPUNKT_JAN_23.minusYears(1)
        val behandling = mockBehandling(virk)
        val grunnlag =
            GrunnlagTestData(opplysningsmapAvdoedOverrides = avdoedOverrides(virk.atDay(1).minusDays(20)))
                .hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery {
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                any(),
                any(),
            )
        } returns
            barnepensjonBeregningsGrunnlag(
                behandling.id,
                emptyList(),
                BeregnBarnepensjonServiceTest.VIRKNINGSTIDSPUNKT_JAN_23.minusYears(1),
            )

        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns null
        every {
            featureToggleService.isEnabled(BeregnBarnepensjonServiceFeatureToggle.BrukFaktiskTrygdetid, false)
        } returns false
        every {
            featureToggleService.isEnabled(
                BeregnBarnepensjonServiceFeatureToggle.BrukInstitusjonsoppholdIBeregning,
                false,
            )
        } returns false

        runBlocking {
            val beregning22 = beregnBarnepensjonService.beregn(behandling, bruker)

            with(beregning22) {
                beregningId shouldNotBe null
                behandlingId shouldBe behandling.id
                type shouldBe Beregningstype.BP
                beregnetDato shouldNotBe null
                grunnlagMetadata shouldBe grunnlag.metadata
                beregningsperioder.size shouldBeGreaterThanOrEqual 3
                with(beregningsperioder.first()) {
                    utbetaltBeloep shouldBe 3547
                    datoFOM shouldBe behandling.virkningstidspunkt?.dato
                    datoTOM shouldBe YearMonth.of(2022, Month.APRIL)
                    grunnbelopMnd shouldBe
                        hentGjeldendeGrunnbeloep(
                            this.datoFOM,
                        ).grunnbeloepPerMaaned
                    soeskenFlokk shouldBe emptyList()
                    trygdetid shouldBe MAKS_TRYGDETID
                    regelResultat shouldNotBe null
                    regelVersjon shouldNotBe null
                }
            }

            val utbetaltBeloep22 = beregning22.beregningsperioder.first().utbetaltBeloep
            val utbetaltBeloep23 = beregning22.beregningsperioder.get(1).utbetaltBeloep
            val faktorUtbetalt = utbetaltBeloep23.toDouble().div(utbetaltBeloep22)

            val grunnbeloep22 = hentGjeldendeGrunnbeloep(YearMonth.of(2022, Month.JANUARY)).grunnbeloepPerMaaned
            val grunnbeloep23 =
                hentGjeldendeGrunnbeloep(BeregnBarnepensjonServiceTest.VIRKNINGSTIDSPUNKT_JAN_23).grunnbeloepPerMaaned
            val faktorGrunnbeloep = grunnbeloep23.toDouble().div(grunnbeloep22)

            Assertions.assertTrue(
                abs(faktorGrunnbeloep - faktorUtbetalt) < 0.0001,
                """Utbetalt 22: $utbetaltBeloep22, utbetalt 23: $utbetaltBeloep23,
                    faktor utbetalt: $faktorUtbetalt,
                    grunnbeløp 22: $grunnbeloep22, grunnbeløp 23: $grunnbeloep23,
                    faktor grunnbeløp: $faktorGrunnbeloep""",
            )
        }
    }

    private fun avdoedOverrides(doedsdato: LocalDate): Map<Opplysningstype, Opplysning<JsonNode>> {
        val kilde = Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, "opplysningsId1")
        val avdoedOverrides =
            doedsdato.toJsonNode().let {
                mapOf(Opplysningstype.DOEDSDATO to Opplysning.Konstant(UUID.randomUUID(), kilde, it))
            }
        return avdoedOverrides
    }

    private fun barnepensjonBeregningsGrunnlag(
        behandlingId: UUID,
        soesken: List<String>,
        virkningstidspunkt: YearMonth = BeregnBarnepensjonServiceTest.VIRKNINGSTIDSPUNKT_JAN_23,
    ) = BeregningsGrunnlag(
        behandlingId,
        mockk {
            every { ident } returns "Z123456"
            every { tidspunkt } returns Tidspunkt.now()
            every { type } returns ""
        },
        soeskenMedIBeregning =
            listOf(
                GrunnlagMedPeriode(
                    fom = virkningstidspunkt.minusMonths(1).atDay(1),
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
        institusjonsoppholdBeregningsgrunnlag =
            listOf(
                GrunnlagMedPeriode(
                    fom = LocalDate.of(2022, 8, 1),
                    tom = null,
                    data = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.NEI_KORT_OPPHOLD),
                ),
            ),
    )

    private fun mockBehandling(virk: YearMonth = BeregnBarnepensjonServiceTest.VIRKNINGSTIDSPUNKT_JAN_23) =
        mockk<DetaljertBehandling>().apply {
            every { id } returns UUID.randomUUID()
            every { sak } returns 1
            every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt(virk)
        }
}
