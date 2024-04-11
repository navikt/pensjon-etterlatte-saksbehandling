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
import java.time.YearMonth
import java.util.UUID

class OmregningHendelserRiverTest {
    @Test
    fun verifiserer() {
        val rapidsConnection = mockk<RapidsConnection>().also { every { it.register(any<River>()) } just runs }
        val beregningService =
            mockk<BeregningService>().also {
            }
        val trygdetidService =
            mockk<TrygdetidService>().also {
                every {
                    it.kopierTrygdetidFraForrigeBehandling(
                        any(),
                        any(),
                    )
                } returns mockk()
            }
        val river = OmregningHendelserRiver(rapidsConnection, beregningService, trygdetidService)

        val nyBehandling = UUID.randomUUID()
        val gammelBehandling = UUID.randomUUID()

        every { beregningService.opprettBeregningsgrunnlagFraForrigeBehandling(nyBehandling, gammelBehandling) } returns mockk()
        every { beregningService.beregn(gammelBehandling) } returns beregningDTO(gammelBehandling, 500)
        every { beregningService.beregn(nyBehandling) } returns beregningDTO(nyBehandling, 1000)

        runBlocking {
            river.beregn(SakType.BARNEPENSJON, behandlingId = nyBehandling, behandlingViOmregnerFra = gammelBehandling)
        }
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
                            every { it.datoFOM } returns YearMonth.now()
                            every { it.utbetaltBeloep } returns nySum
                        },
                    ),
                beregnetDato = Tidspunkt.now(),
                grunnlagMetadata = Metadata(sakId = 1L, versjon = 1L),
                overstyrBeregning = null,
            )
    }
}
