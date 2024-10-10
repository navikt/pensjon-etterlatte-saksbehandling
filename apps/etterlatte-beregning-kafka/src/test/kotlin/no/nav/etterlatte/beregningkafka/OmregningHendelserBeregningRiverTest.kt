package no.nav.etterlatte.beregningkafka

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class OmregningHendelserBeregningRiverTest {
    @Test
    fun `verifiserer naar alt er ok`() {
        val (beregningService, river) = initialiserRiver()

        val nyBehandling = UUID.randomUUID()
        val gammelBehandling = UUID.randomUUID()

        every {
            beregningService.opprettBeregningsgrunnlagFraForrigeBehandling(
                nyBehandling,
                gammelBehandling,
            )
        } returns mockk()
        every { beregningService.tilpassOverstyrtBeregningsgrunnlagForRegulering(nyBehandling) } returns mockk()
        every { beregningService.hentBeregning(gammelBehandling) } returns beregningDTO(gammelBehandling, 500, 1000)
        every { beregningService.beregn(nyBehandling) } returns beregningDTO(nyBehandling, 600, 1100)
        coEvery { beregningService.hentGrunnbeloep() } returns
            Grunnbeloep(
                YearMonth.now(),
                1000,
                100,
                BigDecimal("1.2"),
            )

        runBlocking {
            river.beregn(
                SakType.BARNEPENSJON,
                behandlingId = nyBehandling,
                behandlingViOmregnerFra = gammelBehandling,
            )
        }
    }

    @Test
    fun `feiler naar ny beregning er lavere enn gammel`() {
        val (beregningService, river) = initialiserRiver()

        val nyBehandling = UUID.randomUUID()
        val gammelBehandling = UUID.randomUUID()

        every {
            beregningService.opprettBeregningsgrunnlagFraForrigeBehandling(
                nyBehandling,
                gammelBehandling,
            )
        } returns mockk()
        every { beregningService.tilpassOverstyrtBeregningsgrunnlagForRegulering(nyBehandling) } returns mockk()
        every { beregningService.hentBeregning(gammelBehandling) } returns beregningDTO(gammelBehandling, 1000, 1000)
        every { beregningService.beregn(nyBehandling) } returns beregningDTO(nyBehandling, 500, 1100)
        coEvery { beregningService.hentGrunnbeloep() } returns
            Grunnbeloep(
                YearMonth.now(),
                1000,
                100,
                BigDecimal("1.2"),
            )

        runBlocking {
            assertThrows<MindreEnnForrigeBehandling> {
                river.beregn(
                    SakType.BARNEPENSJON,
                    behandlingId = nyBehandling,
                    behandlingViOmregnerFra = gammelBehandling,
                )
            }
        }
    }

    @Test
    fun `forrige beregning er revurdering fram i tid fra 1 juli, men ny er fra 1 mai, og vi har dermed ikke overlapp paa periode`() {
        val (beregningService, river) = initialiserRiver()

        val nyBehandling = UUID.randomUUID()
        val gammelBehandling = UUID.randomUUID()

        every {
            beregningService.opprettBeregningsgrunnlagFraForrigeBehandling(
                nyBehandling,
                gammelBehandling,
            )
        } returns mockk()
        every { beregningService.tilpassOverstyrtBeregningsgrunnlagForRegulering(nyBehandling) } returns mockk()
        every {
            beregningService.hentBeregning(gammelBehandling)
        } returns beregningDTO(gammelBehandling, 500, 1000, beregningsperiodeFom = YearMonth.of(2024, Month.JULY))
        every { beregningService.beregn(nyBehandling) } returns beregningDTO(nyBehandling, 600, 1100)
        coEvery { beregningService.hentGrunnbeloep() } returns
            Grunnbeloep(
                YearMonth.now(),
                1000,
                100,
                BigDecimal("1.2"),
            )

        runBlocking {
            river.beregn(
                SakType.BARNEPENSJON,
                behandlingId = nyBehandling,
                behandlingViOmregnerFra = gammelBehandling,
            )
        }
    }

    @Test
    fun `ny beregning skal ikke kunne vaere mer enn X prosent hoeyere enn gammel`() {
        val (beregningService, river) = initialiserRiver()

        val nyBehandling = UUID.randomUUID()
        val gammelBehandling = UUID.randomUUID()

        every {
            beregningService.opprettBeregningsgrunnlagFraForrigeBehandling(
                nyBehandling,
                gammelBehandling,
            )
        } returns mockk()
        every { beregningService.tilpassOverstyrtBeregningsgrunnlagForRegulering(nyBehandling) } returns mockk()
        every { beregningService.hentBeregning(gammelBehandling) } returns beregningDTO(gammelBehandling, 1000, 1000)
        every { beregningService.beregn(nyBehandling) } returns beregningDTO(nyBehandling, 1500, 1100)
        coEvery { beregningService.hentGrunnbeloep() } returns
            Grunnbeloep(
                YearMonth.now(),
                1000,
                100,
                BigDecimal("1.2"),
            )

        runBlocking {
            assertThrows<ForStorOekning> {
                river.beregn(
                    SakType.BARNEPENSJON,
                    behandlingId = nyBehandling,
                    behandlingViOmregnerFra = gammelBehandling,
                )
            }
        }
    }

    @Test
    fun `skal runde av endringstallet for aa unngaa ArithmeticException`() {
        val (beregningService, river) = initialiserRiver()

        val nyBehandling = UUID.randomUUID()
        val gammelBehandling = UUID.randomUUID()

        every {
            beregningService.opprettBeregningsgrunnlagFraForrigeBehandling(
                nyBehandling,
                gammelBehandling,
            )
        } returns mockk()
        every { beregningService.tilpassOverstyrtBeregningsgrunnlagForRegulering(nyBehandling) } returns mockk()
        every { beregningService.hentBeregning(gammelBehandling) } returns beregningDTO(gammelBehandling, 7784, 1000)
        every { beregningService.beregn(nyBehandling) } returns beregningDTO(nyBehandling, 7875, 1100)
        coEvery { beregningService.hentGrunnbeloep() } returns
            Grunnbeloep(
                YearMonth.now(),
                1000,
                100,
                BigDecimal("1.01169"),
            )

        runBlocking {
            val resultat =
                river.beregn(
                    SakType.BARNEPENSJON,
                    behandlingId = nyBehandling,
                    behandlingViOmregnerFra = gammelBehandling,
                )

            assertNotNull(resultat)
        }
    }

    private fun initialiserRiver(): Pair<BeregningService, OmregningHendelserBeregningRiver> {
        val rapidsConnection = mockk<RapidsConnection>().also { every { it.register(any<River>()) } just runs }
        val beregningService = mockk<BeregningService>()
        val river = OmregningHendelserBeregningRiver(rapidsConnection, beregningService)
        return Pair(beregningService, river)
    }

    private fun beregningDTO(
        behandling: UUID,
        nySum: Int,
        g: Int,
        beregningsperiodeFom: YearMonth = YearMonth.of(2024, Month.MARCH),
    ) = mockk<HttpResponse>().also {
        coEvery { it.body<BeregningDTO>() } returns
            BeregningDTO(
                beregningId = UUID.randomUUID(),
                behandlingId = behandling,
                type = Beregningstype.BP,
                beregningsperioder =
                    listOf(
                        mockk<Beregningsperiode>().also {
                            every { it.datoFOM } returns beregningsperiodeFom
                            every { it.datoTOM } returns null
                            every { it.utbetaltBeloep } returns nySum
                            every { it.grunnbelop } returns g
                        },
                    ),
                beregnetDato = Tidspunkt.now(),
                grunnlagMetadata = Metadata(sakId = randomSakId(), versjon = 1L),
                overstyrBeregning = null,
            )
    }
}
