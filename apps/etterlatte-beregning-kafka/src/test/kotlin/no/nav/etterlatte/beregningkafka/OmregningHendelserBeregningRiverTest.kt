package no.nav.etterlatte.beregningkafka

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class OmregningHendelserBeregningRiverTest {
    @Test
    fun `verifiserer naar alt er ok`() {
        val (beregningService, river) = initialiserRiver()

        val nyBehandling = UUID.randomUUID()
        val gammelBehandling = UUID.randomUUID()

        every { beregningService.opprettBeregningsgrunnlagFraForrigeBehandling(nyBehandling, gammelBehandling) } returns mockk()
        every { beregningService.tilpassOverstyrtBeregningsgrunnlagForRegulering(nyBehandling) } returns mockk()
        every { beregningService.hentBeregning(gammelBehandling) } returns beregningDTO(gammelBehandling, 500)
        every { beregningService.beregn(nyBehandling) } returns beregningDTO(nyBehandling, 600)

        runBlocking {
            river.beregn(
                SakType.BARNEPENSJON,
                behandlingId = nyBehandling,
                behandlingViOmregnerFra = gammelBehandling,
                LocalDate.of(2024, Month.APRIL, 10),
            )
        }
    }

    @Test
    fun `feiler naar ny beregning er lavere enn gammel`() {
        val (beregningService, river) = initialiserRiver()

        val nyBehandling = UUID.randomUUID()
        val gammelBehandling = UUID.randomUUID()

        every { beregningService.opprettBeregningsgrunnlagFraForrigeBehandling(nyBehandling, gammelBehandling) } returns mockk()
        every { beregningService.tilpassOverstyrtBeregningsgrunnlagForRegulering(nyBehandling) } returns mockk()
        every { beregningService.hentBeregning(gammelBehandling) } returns beregningDTO(gammelBehandling, 1000)
        every { beregningService.beregn(nyBehandling) } returns beregningDTO(nyBehandling, 500)

        runBlocking {
            assertThrows<MindreEnnForrigeBehandling> {
                river.beregn(
                    SakType.BARNEPENSJON,
                    behandlingId = nyBehandling,
                    behandlingViOmregnerFra = gammelBehandling,
                    LocalDate.of(2024, Month.MAY, 10),
                )
            }
        }
    }

    @Test
    fun `ny beregning skal ikke kunne vaere mer enn X prosent hoeyere enn gammel`() {
        val (beregningService, river) = initialiserRiver()

        val nyBehandling = UUID.randomUUID()
        val gammelBehandling = UUID.randomUUID()

        every { beregningService.opprettBeregningsgrunnlagFraForrigeBehandling(nyBehandling, gammelBehandling) } returns mockk()
        every { beregningService.tilpassOverstyrtBeregningsgrunnlagForRegulering(nyBehandling) } returns mockk()
        every { beregningService.hentBeregning(gammelBehandling) } returns beregningDTO(gammelBehandling, 1000)
        every { beregningService.beregn(nyBehandling) } returns beregningDTO(nyBehandling, 1500)

        runBlocking {
            assertThrows<ForStorOekning> {
                river.beregn(
                    SakType.BARNEPENSJON,
                    behandlingId = nyBehandling,
                    behandlingViOmregnerFra = gammelBehandling,
                    LocalDate.of(2024, Month.MAY, 10),
                )
            }
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
    ) = mockk<HttpResponse>().also {
        coEvery { it.body<BeregningDTO>() } returns
            BeregningDTO(
                beregningId = UUID.randomUUID(),
                behandlingId = behandling,
                type = Beregningstype.BP,
                beregningsperioder =
                    listOf(
                        mockk<Beregningsperiode>().also {
                            every { it.datoFOM } returns YearMonth.of(2024, Month.MARCH)
                            every { it.datoTOM } returns null
                            every { it.utbetaltBeloep } returns nySum
                        },
                    ),
                beregnetDato = Tidspunkt.now(),
                grunnlagMetadata = Metadata(sakId = 1L, versjon = 1L),
                overstyrBeregning = null,
            )
    }
}
