package utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Kjoereplan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException

class KjoereplanTest {

    @Test
    fun `kjoereplan toString mapper til riktig oppdragverdi`() {
        assertEquals("J", Kjoereplan.NESTE_PLANLAGTE_UTBETALING.toString())
        assertEquals("N", Kjoereplan.MED_EN_GANG.toString())
    }

    @Test
    fun `Kjoereplan fraVerdi mapper riktig`() {
        assertEquals(Kjoereplan.MED_EN_GANG, Kjoereplan.fraKode("n"))
        assertEquals(Kjoereplan.MED_EN_GANG, Kjoereplan.fraKode(" N "))
        assertEquals(Kjoereplan.NESTE_PLANLAGTE_UTBETALING, Kjoereplan.fraKode(" j "))
        assertEquals(Kjoereplan.NESTE_PLANLAGTE_UTBETALING, Kjoereplan.fraKode("J"))

        assertThrows<IllegalArgumentException> { Kjoereplan.fraKode("b") }
        assertThrows<IllegalArgumentException> { Kjoereplan.fraKode("av og til") }
    }
}