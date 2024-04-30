package no.nav.etterlatte.beregningkafka

import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILMELDING_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class SjekkOmOverstyrtBeregningRiverTest {
    private val beregningService = mockk<BeregningService>()
    private val inspector = TestRapid().apply { SjekkOmOverstyrtBeregningRiver(this, beregningService) }

    @Test
    fun `skal feile om sak har aapen behandling med overstyrt beregning`() {
        val tilbakestilgBehandling = UUID.fromString("63ba95c8-119b-465f-81fa-0a5316451db4")
        every { beregningService.hentOverstyrt(tilbakestilgBehandling) } returns
            mockk<HttpResponse>().also {
                every { it.status } returns HttpStatusCode.OK
            }
        val inspector = inspector.apply { sendTestMessage(fullMelding) }
        inspector.sendTestMessage(fullMelding)

        val melding = inspector.inspektør.message(0)
        assertEquals(EventNames.FEILA.lagEventnameForType(), melding.get(EVENT_NAME_KEY).textValue())
        assertTrue("KanIkkeRegulereSakMedAapenBehandlingOverstyrtBeregning" in melding.get(FEILMELDING_KEY).textValue())
    }

    @Test
    fun `skal fortsette om sak ikke har aapen behandling med overstyrt beregning`() {
        val tilbakestilgBehandling = UUID.fromString("63ba95c8-119b-465f-81fa-0a5316451db4")
        every { beregningService.hentOverstyrt(tilbakestilgBehandling) } returns
            mockk<HttpResponse>().also {
                every { it.status } returns HttpStatusCode.NoContent
            }

        val inspector = inspector.apply { sendTestMessage(fullMelding) }

        assertEquals(inspector.inspektør.size, 1)
        assertEquals(
            inspector.inspektør.message(0).get("@event_name").textValue(),
            ReguleringHendelseType.UTFORT_SJEKK_AAPEN_OVERSTYRT.lagEventnameForType(),
        )
    }

    companion object {
        val fullMelding = readFile("/omregningshendelse_sjekk_overstyrt.json")
    }
}
