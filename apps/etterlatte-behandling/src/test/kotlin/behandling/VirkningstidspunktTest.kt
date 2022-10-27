package behandling

import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

internal class VirkningstidspunktTest {
    private val datetimeformatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    @Test
    fun `lager virkningstidspunkt fra og med foerste i neste maanad`() {
        val localDate = LocalDate.parse("05-05-2022", datetimeformatter)

        val expected = YearMonth.parse("01-06-2022", datetimeformatter)
        val actual = Virkningstidspunkt.foersteNesteMaanad(localDate).dato

        Assertions.assertEquals(expected, actual)
    }
}