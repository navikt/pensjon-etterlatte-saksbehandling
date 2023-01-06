package sporingslogg

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime

internal class SporingsloggerTest {

    @Test
    fun `skal logge i CEF-format`() {
        /* Formatet: CEF:Version|Device Vendor|Device Product|Device Version|Device Event Class ID|Name|Severity|[Extension] */
        val timestamp =
            ZonedDateTime.of(
                LocalDate.of(2020, Month.FEBRUARY, 1),
                LocalTime.NOON,
                ZoneId.of("Europe/Oslo")
            ).toInstant()
        val entry = CEFEntry(
            deviceEventClassId = DeviceEventClassId.Access,
            deviceProduct = "vilkaarsvurdering",
            name = Name.OnBehalfOfAccess,
            severity = Severity.INFO,
            extension = Extension(
                endTime = timestamp,
                sourceUserId = "U123456",
                destinationUserId = "10458210115",
                sourceProcessName = "vilkaarsvurdering",
                flexString1 = Decision.Permit,
                message = "Internbruker sjekker barnepensjon for innbygger"
            )
        )
        assertEquals(
            "CEF:0|pensjon|etterlatte-vilkaarsvurdering|1.0|audit:access|On-behalf-of access|INFO|" +
                "end=1580554800000 suid=U123456 duid=10458210115 sproc=vilkaarsvurdering " +
                "flexString1Label=Decision flexString1=Permit msg=Internbruker sjekker barnepensjon for innbygger",
            entry.format()
        )
    }
}