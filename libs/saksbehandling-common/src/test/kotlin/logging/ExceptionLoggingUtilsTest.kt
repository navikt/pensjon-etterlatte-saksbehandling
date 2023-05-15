package logging

import no.nav.etterlatte.libs.common.logging.samleExceptions
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ExceptionLoggingUtilsTest {

    @Test
    fun `exceptions blir lagt til i lista som suppressed, den nyeste foerst`() {
        val e1 = RuntimeException("e1")
        val e2 = RuntimeException("e2")
        val e3 = RuntimeException("e3")

        val samla = samleExceptions(listOf(e1, e2, e3))
        Assertions.assertEquals(e3, samla)
        Assertions.assertEquals(listOf(e2, e1), samla.suppressedExceptions)
    }
}