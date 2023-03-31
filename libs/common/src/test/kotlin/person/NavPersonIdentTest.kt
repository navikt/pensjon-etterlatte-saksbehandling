package no.nav.etterlatte.person

import no.nav.etterlatte.libs.common.person.NavPersonIdent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class NavPersonIdentTest {

    @Test
    fun `NPID sjekker om den er 11 siffer lang`() {
        assertThrows<IllegalArgumentException> { NavPersonIdent("1234567890") }
        assertThrows<IllegalArgumentException> { NavPersonIdent("123456789011") }
        assertThrows<IllegalArgumentException> { NavPersonIdent(" 12312312312 ") }
        assertThrows<IllegalArgumentException> { NavPersonIdent("aaaaaa") }
        assertDoesNotThrow { NavPersonIdent("01219000000") }
    }

    @Test
    fun `NPID sjekker om den månedsdelen av identifikatoren er 20 + måned eller 60 + måned for test`() {
        assertThrows<IllegalArgumentException> { NavPersonIdent("12335678901") }
        assertThrows<IllegalArgumentException> { NavPersonIdent("12125678901") }
        assertThrows<IllegalArgumentException> { NavPersonIdent("12605678901") }
        assertThrows<IllegalArgumentException> { NavPersonIdent("12735678901") }
        assertDoesNotThrow { NavPersonIdent("01309000000") }
        assertDoesNotThrow { NavPersonIdent("01329000000") }
        assertDoesNotThrow { NavPersonIdent("01289000000") }
        assertDoesNotThrow { NavPersonIdent("01619000000") }
        assertDoesNotThrow { NavPersonIdent("01729000000") }
    }
}