package no.nav.etterlatte.beregning.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SoeskenjusteringTest {
    @Test
    fun `Soeskenjustering skal gi rett, avrundet beløp for 0 søsken`() {
        assertEquals(3716, Soeskenjustering(0, 111477).beloep)
    }

    @Test
    fun `Soeskenjustering skal gi rett, avrundet beløp for 1 søsken`() {
        assertEquals(3019, Soeskenjustering(1, 111477).beloep)
    }

    @Test
    fun `Soeskenjustering skal gi rett, avrundet beløp for 2 søsken`() {
        assertEquals(2787, Soeskenjustering(2, 111477).beloep)
    }
}