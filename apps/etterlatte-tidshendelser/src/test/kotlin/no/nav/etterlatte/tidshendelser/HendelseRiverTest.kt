package no.nav.etterlatte.tidshendelser

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_STEG_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_TYPE_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

class HendelseRiverTest {
    private val hendelseDao = mockk<HendelseDao>()
    private val inspector = TestRapid().apply { HendelseRiver(this, hendelseDao) }

    @Test
    fun `skal lese melding og sjekke loepende ytelse`() {
        every { hendelseDao.oppdaterHendelseForSteg("123-123-123", "VURDERT_LOPENDE_YTELSE") } returns Unit

        val melding =
            JsonMessage.newMessage(
                EventNames.ALDERSOVERGANG.name,
                mapOf(
                    ALDERSOVERGANG_STEG_KEY to "VURDERT_LOPENDE_YTELSE",
                    ALDERSOVERGANG_TYPE_KEY to "BP20",
                    ALDERSOVERGANG_ID_KEY to "123-123-123",
                    SAK_ID_KEY to 8763L,
                ),
            )

        inspector.apply { sendTestMessage(melding.toJson()) }

        verify { hendelseDao.oppdaterHendelseForSteg("123-123-123", "VURDERT_LOPENDE_YTELSE") }
    }
}
