import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.database.toList
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.sql.ResultSet

class JdbcUtilsKtTest {
    @Test
    fun `toList stopper ikke hvis block() gir null og resultset har verdier`() {
        val resultSet: ResultSet = mockk()
        every {
            resultSet.next()
        } returnsMany listOf(true, true, true, true, false)

        val block: ResultSet.() -> String? = mockk()
        every { resultSet.block() } returnsMany listOf("1", "2", null, "4")

        val liste = resultSet.toList(block)
        Assertions.assertEquals(liste, listOf("1", "2", null, "4"))
    }
}
