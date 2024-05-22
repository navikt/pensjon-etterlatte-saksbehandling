import no.nav.etterlatte.createRecord
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class ApplicationKtTest {
    @Test
    fun `lager record`() {
        val record = createRecord(LocalDate.of(2024, Month.MAY, 21))
        println(record)
    }
}
