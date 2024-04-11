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
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.junit.jupiter.api.Test
import java.util.UUID

class OmregningHendelserRiverTest {
    @Test
    fun verifiserer() {
        val rapidsConnection = mockk<RapidsConnection>().also { every { it.register(any<River>()) } just runs }
        val beregningService = mockk<BeregningService>()
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

        every {
            beregningService.beregn(
                nyBehandling,
            )
        } returns mockk<HttpResponse>().also { coEvery { it.body<BeregningDTO>() } returns mockk() }
        every {
            beregningService.beregn(gammelBehandling)
        } returns mockk<HttpResponse>().also { coEvery { it.body<BeregningDTO>() } returns mockk() }

        runBlocking {
            river.beregn(SakType.BARNEPENSJON, behandlingId = nyBehandling, behandlingViOmregnerFra = gammelBehandling)
        }
    }
}
