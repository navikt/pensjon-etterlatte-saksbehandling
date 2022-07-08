package no.nav.etterlatte.hendelserpdl

import no.nav.etterlatte.hendelserpdl.leesah.ILivetErEnStroemAvHendelser
import no.nav.person.pdl.leesah.Personhendelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test


internal class FinnDodsmeldingerTest {

    @Test
    fun ikkeDodsmelding() {
        val subject =
            FinnDodsmeldinger(LeesahMock(listOf(Personhendelse().apply { put("opplysningstype", "Ikke dodsmelding") })),
                DodsMock { _, _ -> fail() })
        subject.stream()
    }

    @Test
    fun dodsmelding() {
        val dodsmeldinger = mutableListOf<String>()
        val subject = FinnDodsmeldinger(LeesahMock(listOf(Personhendelse().apply {
            put("opplysningstype", "DOEDSFALL_V1")
            put("personidenter", listOf("123"))
        })), DodsMock { it, _ -> dodsmeldinger += it })
        subject.stream()
        assertEquals(1, dodsmeldinger.size)
        assertEquals("123", dodsmeldinger[0])
    }

}

class LeesahMock(val mockData: List<Personhendelse>) : ILivetErEnStroemAvHendelser {
    override fun poll(c: (Personhendelse) -> Unit): Int {
        mockData.forEach(c)
        return 1
    }

    override fun fraStart() {

    }
}

class DodsMock(val c: (String, String?) -> Unit) : IDodsmeldinger {
    override fun personErDod(ident: String, doedsdato: String?) = c(ident, doedsdato)

}
