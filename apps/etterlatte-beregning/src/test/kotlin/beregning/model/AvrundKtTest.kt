package no.nav.etterlatte.beregning.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class AvrundKtTest {

    @Test
    fun `avrunder til nermest krone`() {
        assertEquals(100, avrund(BigDecimal.valueOf(99.6)))
        assertEquals(99, avrund(BigDecimal.valueOf(99.4)))
    }

    @Test
    fun `avrunder opp`() {
        assertEquals(100, avrund(BigDecimal.valueOf(99.5)))
    }
}