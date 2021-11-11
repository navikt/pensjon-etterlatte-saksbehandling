package person

import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.InvalidFoedselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

internal class FoedselsnummerTest {

    @Test
    fun `Sjekk diverse gyldige test fnr`() {
        val gyldigeFnrListe = listOf(
            "11057523044", "26117512737", "26104500284", "24116324268", "04096222195", "05126307952"
        )

        assertAll(
            gyldigeFnrListe.map {
                { assertEquals(it, Foedselsnummer.of(it).value) }
            }
        )
    }

    @Test
    fun `Sjekk diverse gyldige test fnr med mellomrom eller bindestrek`() {
        assertAll(
            { assertEquals("11057523044", Foedselsnummer.of("110575 23044").value) },
            { assertEquals("11057523044", Foedselsnummer.of("110575-23044").value) },
            { assertEquals("26117512737", Foedselsnummer.of("26 11 75 12737").value) },
            { assertEquals("26117512737", Foedselsnummer.of("26-11-75-12737").value) },
            { assertEquals("26104500284", Foedselsnummer.of(" 26104500284 ").value) },
            { assertEquals("24116324268", Foedselsnummer.of(" 2 4 1 1 6 3 2 4 2 6 8 ").value) },
            { assertEquals("05126307952", Foedselsnummer.of(" 05   126    307   952").value) }
        )
    }

    @Test
    fun `Sjekk diverse ugyldige numeriske verdier`() {
        assertThrows<InvalidFoedselsnummer> {
            Foedselsnummer.of("1234")
        }

        val ugyldigeFnrListe = listOf(
            "00000000000", "11111111111", "22222222222", "33333333333", "44444444444",
            "55555555555", "66666666666", "77777777777", "88888888888", "99999999999",
            "36117512737", "12345678901", "00000000001", "10000000000"
        )

        assertAll(
            ugyldigeFnrListe.map {
                { assertThrows<InvalidFoedselsnummer> { Foedselsnummer.of(it) } }
            }
        )
    }

    @Test
    fun `Sjekk diverse ugyldige tekst verdier`() {
        assertThrows<InvalidFoedselsnummer> { Foedselsnummer.of("") }
        assertThrows<InvalidFoedselsnummer> { Foedselsnummer.of("hei") }
    }

    @Test
    fun `Foedselsnummer sin toString anonymiserer`() {
        val fnr = Foedselsnummer.of("24014021406")

        assertEquals("240140*****", fnr.toString())
    }
}
