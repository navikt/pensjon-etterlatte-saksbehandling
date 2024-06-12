package no.nav.etterlatte.person

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.InvalidFoedselsnummerException
import no.nav.etterlatte.libs.common.person.firesifretAarstallFraTosifret
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class FoedselsnummerTest {
    @Test
    fun `Sjekk diverse gyldige test fnr`() {
        val gyldigeFnrListe =
            listOf(
                "02438311109",
                "18498248795",
                "09438336165",
                "31488338237",
                "16508201382",
                "27458328671",
            )

        assertAll(
            gyldigeFnrListe.map {
                { assertEquals(it, Folkeregisteridentifikator.of(it).value) }
            },
        )
    }

    @Test
    fun `Sjekk diverse gyldige test fnr med mellomrom eller bindestrek`() {
        assertAll(
            { assertEquals("27458328671", Folkeregisteridentifikator.of("274583 28671").value) },
            { assertEquals("27458328671", Folkeregisteridentifikator.of("274583-28671").value) },
            { assertEquals("31488338237", Folkeregisteridentifikator.of("31 48 83 38237").value) },
            { assertEquals("31488338237", Folkeregisteridentifikator.of("31-48-83-38237").value) },
            { assertEquals("02438311109", Folkeregisteridentifikator.of(" 02438311109 ").value) },
            { assertEquals("08481376816", Folkeregisteridentifikator.of(" 0 8 4 8 1 3 7 6 8 1 6 ").value) },
            { assertEquals("09438336165", Folkeregisteridentifikator.of(" 09   438    336   165").value) },
        )
    }

    @Test
    fun `Sjekk diverse ugyldige numeriske verdier`() {
        assertThrows<InvalidFoedselsnummerException> {
            Folkeregisteridentifikator.of("1234")
        }

        val ugyldigeFnrListe =
            listOf(
                "00000000000",
                "11111111111",
                "22222222222",
                "33333333333",
                "44444444444",
                "55555555555",
                "66666666666",
                "77777777777",
                "88888888888",
                "99999999999",
                "36117512737",
                "12345678901",
                "00000000001",
                "10000000000",
            )

        assertAll(
            ugyldigeFnrListe.map {
                { assertThrows<InvalidFoedselsnummerException> { Folkeregisteridentifikator.of(it) } }
            },
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
    fun `Null er ikke gyldig`() {
        assertFalse(Folkeregisteridentifikator.isValid(null))
    }

    @Test
    fun `Foedselsnummer sin toString anonymiserer`() {
        val fnr = Folkeregisteridentifikator.of("08498224343")
        assertEquals("084982*****", fnr.toString())
    }
}
