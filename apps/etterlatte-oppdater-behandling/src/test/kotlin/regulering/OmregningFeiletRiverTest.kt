package no.nav.etterlatte.regulering

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames.FEILA
import no.nav.etterlatte.rapidsandrivers.KONTEKST_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.RapidEvents
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OmregningFeiletRiverTest {
    private val foersteMai2023 = LocalDate.of(2023, 5, 1)

    private fun genererReguleringMelding() =
        JsonMessage.newMessage(
            mapOf(
                FEILA.lagParMedEventNameKey(),
                DATO_KEY to foersteMai2023,
                KONTEKST_KEY to Kontekst.REGULERING,
                SAK_ID_KEY to 83L,
                RapidEvents.KJOERING to "Regulering2023",
                RapidEvents.ANTALL to Int.MAX_VALUE,
            ),
        )

    @Test
    fun `Skal varsle behandling om at det er en feilet regulering i en sak`() {
        val kjoering = slot<String>()
        val sakId = slot<SakId>()
        val status = slot<KjoeringStatus>()

        val melding = genererReguleringMelding()
        val behandlingService = mockk<BehandlingService>(relaxed = true)
        every { behandlingService.lagreKjoering(capture(sakId), capture(status), capture(kjoering)) } returns Unit
        val inspector = TestRapid().apply { OmregningFeiletRiver(this, behandlingService) }

        inspector.sendTestMessage(melding.toJson())
        val sendteMeldinger = inspector.inspektør.size
        Assertions.assertEquals(0, sendteMeldinger)
        Assertions.assertEquals("Regulering2023", kjoering.captured)
        Assertions.assertEquals(83, sakId.captured)
        Assertions.assertEquals(KjoeringStatus.FEILA, status.captured)
    }
}
