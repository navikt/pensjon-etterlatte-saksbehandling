package no.nav.etterlatte.person

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.InvalidFoedselsnummerException
import no.nav.etterlatte.libs.common.person.firesifretAarstallFraTosifret
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class FoedselsnummerTest {

    @Test
    fun `Sjekk diverse gyldige test fnr`() {
        val gyldigeFnrListe = listOf(
            "11057523044",
            "26117512737",
            "26104500284",
            "24116324268",
            "04096222195",
            "05126307952"
        )

        assertAll(
            gyldigeFnrListe.map {
                { assertEquals(it, Folkeregisteridentifikator.of(it).value) }
            }
        )
    }

    @Test
    fun `Sjekk diverse gyldige test fnr med mellomrom eller bindestrek`() {
        assertAll(
            { assertEquals("11057523044", Folkeregisteridentifikator.of("110575 23044").value) },
            { assertEquals("11057523044", Folkeregisteridentifikator.of("110575-23044").value) },
            { assertEquals("26117512737", Folkeregisteridentifikator.of("26 11 75 12737").value) },
            { assertEquals("26117512737", Folkeregisteridentifikator.of("26-11-75-12737").value) },
            { assertEquals("26104500284", Folkeregisteridentifikator.of(" 26104500284 ").value) },
            { assertEquals("24116324268", Folkeregisteridentifikator.of(" 2 4 1 1 6 3 2 4 2 6 8 ").value) },
            { assertEquals("05126307952", Folkeregisteridentifikator.of(" 05   126    307   952").value) }
        )
    }

    @Test
    fun `Sjekk diverse ugyldige numeriske verdier`() {
        assertThrows<InvalidFoedselsnummerException> {
            Folkeregisteridentifikator.of("1234")
        }

        val ugyldigeFnrListe = listOf(
            "00000000000", "11111111111", "22222222222", "33333333333", "44444444444",
            "55555555555", "66666666666", "77777777777", "88888888888", "99999999999",
            "36117512737", "12345678901", "00000000001", "10000000000"
        )

        assertAll(
            ugyldigeFnrListe.map {
                { assertThrows<InvalidFoedselsnummerException> { Folkeregisteridentifikator.of(it) } }
            }
        )
    }

    @Test
    fun `fødselsmåned for H-identer finner fødselsdatoer riktig`() {
        val hident = Folkeregisteridentifikator.of("18528117224")
        val hidentForDnummer = Folkeregisteridentifikator.of("51447721728")
        assertEquals(LocalDate.of(1981, 12, 18), hident.getBirthDate())
        assertEquals(LocalDate.of(1977, 4, 11), hidentForDnummer.getBirthDate())
    }

    @Test
    fun `fødselsmåned for syntetiske skatteetaten identer finner fødselsdatoer riktig`() {
        val skattdent = Folkeregisteridentifikator.of("30901699972")
        assertEquals(LocalDate.of(2016, 10, 30), skattdent.getBirthDate())
    }

    @Test
    fun `korrekt logikk for firesifret foedselsnummer fra fnr`() {
        assertEquals(2019, firesifretAarstallFraTosifret(19, 999))
        assertEquals(1940, firesifretAarstallFraTosifret(40, 999))
        assertEquals(1919, firesifretAarstallFraTosifret(19, 499))
        assertEquals(1940, firesifretAarstallFraTosifret(40, 1))
        assertEquals(1855, firesifretAarstallFraTosifret(55, 500))
        assertEquals(1855, firesifretAarstallFraTosifret(55, 749))
        assertEquals(2039, firesifretAarstallFraTosifret(39, 751))
        assertEquals(1940, firesifretAarstallFraTosifret(40, 900))
    }

    @Test
    fun `Sjekk diverse ugyldige tekst verdier`() {
        assertThrows<InvalidFoedselsnummerException> { Folkeregisteridentifikator.of("") }
        assertThrows<InvalidFoedselsnummerException> { Folkeregisteridentifikator.of("hei") }
    }

    @Test
    fun `Foedselsnummer sin toString anonymiserer`() {
        val fnr = Folkeregisteridentifikator.of("24014021406")
        assertEquals("240140*****", fnr.toString())
    }
}