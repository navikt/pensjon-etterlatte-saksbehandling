import no.nav.etterlatte.brev.EtterlatteBrevKode
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EtterlatteBrevKodeTest {
    @Test
    fun `ingen brevkoder har lengde over 50`() {
        EtterlatteBrevKode.entries.filter { it.name.length > 50 }.let {
            assertTrue(it.isEmpty(), "Alle brevkoder må være under 50 lange for å kunne arkiveres. Disse feila: $it")
        }
    }
}
