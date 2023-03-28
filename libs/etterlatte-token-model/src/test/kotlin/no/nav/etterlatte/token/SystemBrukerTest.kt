package no.nav.etterlatte.token

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SystemBrukerTest {

    @Test
    fun `systembrukers enhet er fire tegn`() {
        assertEquals(4, SystemBruker("a", "b").saksbehandlerEnhet(mapOf()).length)
    }
}