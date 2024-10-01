package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class EnhetsnummerTest {
    @Test
    fun `Kontroller noen gyldige enhetsnummer`() {
        assertEquals("0123", Enhetsnummer("0123").enhetNr)
        assertEquals("0000", Enhetsnummer("0000").enhetNr)
        assertEquals("4808", Enhetsnummer("4808").enhetNr)
    }

    @Test
    fun `Sjekk noen ugyldige enhetsnummer`() {
        assertThrows<IllegalArgumentException> {
            Enhetsnummer("ABCD") // 4 tegn, men ikke siffer
        }

        assertThrows<IllegalArgumentException> {
            Enhetsnummer("1") // Kun 1 siffer
        }

        assertThrows<IllegalArgumentException> {
            Enhetsnummer("") // Tom string
        }
    }

    @Test
    fun `Sjekk serde fungerer`() {
        val initial = Enhetsnummer("4808")

        val json = objectMapper.writeValueAsString(initial)
        assertEquals("\"4808\"", json)

        val serialized = objectMapper.readValue(json, jacksonTypeRef<Enhetsnummer>())

        assertEquals(initial, serialized)
    }
}
