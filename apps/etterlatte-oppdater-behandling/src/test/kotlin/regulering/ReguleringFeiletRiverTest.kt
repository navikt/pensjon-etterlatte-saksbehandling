package no.nav.etterlatte.regulering

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.ReguleringFeiletHendelse
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames.FEILA
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.AARSAK
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ReguleringFeiletRiverTest {
    private val foersteMai2023 = LocalDate.of(2023, 5, 1)

    private fun genererReguleringMelding() =
        JsonMessage.newMessage(
            mapOf(
                FEILA.lagParMedEventNameKey(),
                DATO_KEY to foersteMai2023,
                AARSAK to ReguleringEvents.EVENT_NAME,
                SAK_ID_KEY to 83L,
            ),
        )

    @Test
    fun `Skal varsle behandling om at det er en feilet regulering i en sak`() {
        val sendtHendelse = slot<ReguleringFeiletHendelse>()
        val melding = genererReguleringMelding()
        val behandlingService = mockk<BehandlingService>(relaxed = true)
        every { behandlingService.sendReguleringFeiletHendelse(capture(sendtHendelse)) } returns Unit
        val inspector = TestRapid().apply { ReguleringFeiletRiver(this, behandlingService) }

        inspector.sendTestMessage(melding.toJson())
        val sendteMeldinger = inspector.inspektør.size
        Assertions.assertEquals(0, sendteMeldinger)
        Assertions.assertEquals(83, sendtHendelse.captured.sakId)
    }
}
