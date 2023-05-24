package utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Kjoereplan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KjoereplanTest {

    @Test
    fun `kjoereplan toString mapper til riktig oppdragverdi`() {
        assertEquals("J", Kjoereplan.NESTE_PLANLAGTE_UTBETALING.toString())
        assertEquals("N", Kjoereplan.MED_EN_GANG.toString())
    }
}