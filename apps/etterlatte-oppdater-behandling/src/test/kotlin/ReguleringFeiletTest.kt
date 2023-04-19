import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.ReguleringFeilet
import no.nav.etterlatte.ReguleringFeiletHendelse
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames.FEILA
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import rapidsandrivers.DATO_KEY
import java.time.LocalDate

internal class ReguleringFeiletTest {

    private val `1_mai_2023` = LocalDate.of(2023, 5, 1)

    private fun genererReguleringMelding() = JsonMessage.newMessage(
        mapOf(
            EVENT_NAME_KEY to FEILA,
            DATO_KEY to `1_mai_2023`,
            "aarsak" to "REGULERING",
            "sakId" to 83L
        )
    )

    @Test
    fun `Skal varsle behandling om at det er en feilet regulering i en sak`() {
        val sendtHendelse = slot<ReguleringFeiletHendelse>()
        val melding = genererReguleringMelding()
        val behandlingService = mockk<BehandlingService>(relaxed = true)
        every { behandlingService.sendReguleringFeiletHendelse(capture(sendtHendelse)) } returns Unit
        val inspector = TestRapid().apply { ReguleringFeilet(this, behandlingService) }

        inspector.sendTestMessage(melding.toJson())
        val sendteMeldinger = inspector.inspektør.size
        Assertions.assertEquals(0, sendteMeldinger)
        Assertions.assertEquals(83, sendtHendelse.captured.sakId)
    }
}