package no.nav.etterlatte.pdl.mapper

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.pdl.PdlGradering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class AdressebeskyttelseTest {
    private val mapper = objectMapper

    @Test
    fun `Verifiser at gradering har korrekt rekkefoelge`() {
        assertEquals(0, PdlGradering.STRENGT_FORTROLIG_UTLAND.ordinal)
        assertEquals(1, PdlGradering.STRENGT_FORTROLIG.ordinal)
        assertEquals(2, PdlGradering.FORTROLIG.ordinal)
        assertEquals(3, PdlGradering.UGRADERT.ordinal)
    }

    @Test
    fun `Verifiser prioritering av gradering`() {
        val graderinger1 =
            listOf(
                PdlGradering.UGRADERT,
                PdlGradering.STRENGT_FORTROLIG,
                PdlGradering.STRENGT_FORTROLIG_UTLAND,
                PdlGradering.FORTROLIG,
            )
        assertEquals(PdlGradering.STRENGT_FORTROLIG_UTLAND, graderinger1.minOrNull())

        val graderinger2 =
            listOf(
                PdlGradering.FORTROLIG,
                PdlGradering.STRENGT_FORTROLIG,
                PdlGradering.UGRADERT,
            )
        assertEquals(PdlGradering.STRENGT_FORTROLIG, graderinger2.minOrNull())

        val graderinger3 =
            listOf(
                PdlGradering.FORTROLIG,
                PdlGradering.UGRADERT,
            )
        assertEquals(PdlGradering.FORTROLIG, graderinger3.minOrNull())

        val graderinger4 =
            listOf(
                PdlGradering.UGRADERT,
            )
        assertEquals(PdlGradering.UGRADERT, graderinger4.minOrNull())

        val graderinger5 = emptyList<PdlGradering>()
        assertNull(graderinger5.minOrNull())
    }

    @Test
    fun `Sjekk at valueOf paa gradering fungerer som forventet`() {
        assertEquals(PdlGradering.STRENGT_FORTROLIG_UTLAND, PdlGradering.valueOf("STRENGT_FORTROLIG_UTLAND"))
        assertEquals(PdlGradering.STRENGT_FORTROLIG, PdlGradering.valueOf("STRENGT_FORTROLIG"))
        assertEquals(PdlGradering.FORTROLIG, PdlGradering.valueOf("FORTROLIG"))
        assertEquals(PdlGradering.UGRADERT, PdlGradering.valueOf("UGRADERT"))

        assertThrows<Throwable> { PdlGradering.valueOf("ukjent") }
    }

    @Test
    fun `Sjekk at serde av gradering fungerer som forventet`() {
        val serialized = "\"FORTROLIG\""
        val deserialized = mapper.readValue(serialized, jacksonTypeRef<PdlGradering>())

        assertEquals(PdlGradering.FORTROLIG, deserialized)

        val reSerialized = mapper.writeValueAsString(deserialized)
        assertEquals(serialized, reSerialized)
    }
}
