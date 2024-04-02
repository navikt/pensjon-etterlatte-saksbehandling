package no.nav.etterlatte.tidshendelser

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.OpeningHours
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.ZonedDateTime

class JobbPollerTest {
    @Nested
    inner class OpeningHoursTest {
        private val openingHours08to16 = OpeningHours.of("08-16")

        @Test
        fun `kl 8 er i aapningstida 8-16`() {
            val noon = ZonedDateTime.now().withHour(12).toInstant()
            openingHours08to16.isOpen(Clock.fixed(noon, norskTidssone)) shouldBe true
        }

        @Test
        fun `kl bittelitt over 16 er utenfor aapningstida 8-16`() {
            val sekstenish = ZonedDateTime.now().withHour(16).withMinute(0).withSecond(1).toInstant()
            openingHours08to16.isOpen(Clock.fixed(sekstenish, norskTidssone)) shouldBe false
        }

        @Test
        fun `midnatt er utenfor aapningstida 8-16`() {
            val midnatt = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(1).toInstant()
            openingHours08to16.isOpen(Clock.fixed(midnatt, norskTidssone)) shouldBe false
        }
    }
}
