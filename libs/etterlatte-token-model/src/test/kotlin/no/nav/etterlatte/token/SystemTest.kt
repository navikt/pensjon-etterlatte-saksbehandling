package no.nav.etterlatte.token

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SystemTest {

    @Test
    fun `systembrukers enhet er fire tegn`() {
        assertEquals(4, System("a", "b").saksbehandlerEnhet(mapOf()).length)
    }
}