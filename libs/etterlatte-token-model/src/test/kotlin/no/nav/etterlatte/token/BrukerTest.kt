package no.nav.etterlatte.token

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class BrukerTest {

    @Test
    fun `er maskin-til-maskin viss oid og sub er like`() {
        assertTrue(Bruker.of("a", "b", "c", "c") is System)
    }

    @Test
    fun `er ikke maskin-til-maskin viss oid og sub er ulike`() {
        assertFalse(Bruker.of("a", "b", "c", "d") is System)
    }

    @Test
    fun `er ikke maskin-til-maskin viss oid er null, men sub har verdi`() {
        assertFalse(Bruker.of("a", "b", null, "d") is System)
    }

    @Test
    fun `er ikke maskin-til-maskin viss sub er null, men oid har verdi`() {
        assertFalse(Bruker.of("a", "b", "c", null) is System)
    }

    @Test
    fun `er ikke maskin-til-maskin viss både oid og sub er null`() {
        assertFalse(Bruker.of("a", "b", null, null) is System)
    }
}