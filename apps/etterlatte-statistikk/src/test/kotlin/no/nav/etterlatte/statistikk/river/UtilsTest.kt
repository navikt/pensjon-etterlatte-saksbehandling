package no.nav.etterlatte.statistikk.river

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.Month

class UtilsTest {
    @Test
    fun `kan parse ZonedDateTime`() {
        val format = "2023-11-22T08:42:18.892199Z"
        val tid = parseTekniskTid(format, "event1")
        Assertions.assertEquals(LocalDateTime.of(2023, Month.NOVEMBER, 22, 8, 42, 18, 892199000), tid)
    }

    @Test
    fun `kan parse LocalDateTime`() {
        val format = "2023-11-22T08:42:18.892199"
        val tid = parseTekniskTid(format, "event1")
        Assertions.assertEquals(LocalDateTime.of(2023, Month.NOVEMBER, 22, 8, 42, 18, 892199000), tid)
    }
}
