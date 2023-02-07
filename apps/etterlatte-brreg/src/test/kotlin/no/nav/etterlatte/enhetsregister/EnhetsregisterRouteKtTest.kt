package no.nav.etterlatte.enhetsregister

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class EnhetsregisterRouteKtTest {

    @Test
    fun `url i inputnavn blir haandtert`() {
        val url = "http://localhost:123/mistenkelig"
        val vaska = vaskInput(url) { "" }
        assertEquals("localhost:123/mistenkelig", vaska)
    }

    @Test
    fun `vanleg inputnavn blir haandtert uten endring`() {
        val url = "Lorem Ipsum Dolor"
        val vaska = vaskInput(url) { "" }
        assertEquals(url, vaska)
    }

    @Test
    fun `orgnr er kun tall`() {
        val orgnr = "tull     1    \t 2     3 :/!\\\\"
        val vaska = vaskOrgnr(orgnr) { "" }
        assertEquals("123", vaska)
    }
}