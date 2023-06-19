package person

import no.nav.etterlatte.libs.common.person.maskerFnr
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MaskerFnrTest {

    @Test
    fun `maskerFnr bytter ut personnummer med xxxxx`() {
        assertEquals("114682*****", "11468227004".maskerFnr())
        assertEquals("134882*****", "13488246113".maskerFnr())
    }

    @Test
    fun `maskerFnr håndterer kortere eller lengre (korrupte) fnr`() {
        assertEquals("*****", "".maskerFnr())
        assertEquals("*****", "123".maskerFnr())
        assertEquals("134882*****", "134882".maskerFnr())
        assertEquals("134882*****", "1348824611313488246113".maskerFnr())
    }
}