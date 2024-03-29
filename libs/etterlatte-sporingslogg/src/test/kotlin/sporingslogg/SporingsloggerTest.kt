package no.nav.etterlatte.libs.sporingslogg

import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month

internal class SporingsloggerTest {
    private val timestamp = LocalDateTime.of(LocalDate.of(2020, Month.FEBRUARY, 1), LocalTime.NOON).toTidspunkt()

    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `skal logge i CEF-format`() {
        val entry =
            CEFEntry(
                deviceEventClassId = DeviceEventClassId.Access,
                deviceProduct = "vilkaarsvurdering",
                name = Name.OnBehalfOfAccess,
                severity = Severity.INFO,
                extension =
                    Extension(
                        endTime = timestamp,
                        sourceUserId = "U123456",
                        destinationUserId = "10458210115",
                        flexString1 = Decision.Permit,
                        message = "Internbruker sjekker barnepensjon for innbygger",
                    ),
            )
        assertEquals(
            "CEF:0|pensjon|etterlatte-vilkaarsvurdering|1.0|audit:access|On-behalf-of access|INFO|end=1580558400000 suid=U123456 duid=10458210115 flexString1Label=Decision flexString1=Permit msg=Internbruker sjekker barnepensjon for innbygger",
            entry.format(),
        )
    }

    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `kan logge manglende tilgang`() {
        val entry =
            CEFEntry(
                deviceEventClassId = DeviceEventClassId.Update,
                deviceProduct = "vilkaarsvurdering",
                name = Name.OnBehalfOfAccess,
                severity = Severity.WARN,
                extension =
                    Extension(
                        endTime = timestamp,
                        sourceUserId = "U123456",
                        destinationUserId = "10458210115",
                        flexString1 = Decision.Deny,
                        message = "Internbruker oppdaterer barnepensjon for innbygger",
                    ),
            )
        assertEquals(
            "CEF:0|pensjon|etterlatte-vilkaarsvurdering|1.0|audit:update|On-behalf-of access|WARN|end=1580558400000 suid=U123456 duid=10458210115 flexString1Label=Decision flexString1=Deny msg=Internbruker oppdaterer barnepensjon for innbygger",
            entry.format(),
        )
    }
}
