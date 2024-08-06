package no.nav.etterlatte.libs.ktor.feilhaandtering

import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktor.feilhaandtering.EscapeUtils.escape
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EscapeUtilsTest {
    @Test
    fun `escaping fungerer`() {
        assertEquals("", escape(""))
        assertEquals("a", escape("a"))
        assertEquals(" a b ", escape(" a b "))
        val str = escape(objectMapper.writeValueAsString(EnkelKlasse("hemmelig")))
        assertEquals("{\\\"ident\\\":\\\"hemmelig\\\"}", str)
    }
}

private data class EnkelKlasse(
    val ident: String,
)
