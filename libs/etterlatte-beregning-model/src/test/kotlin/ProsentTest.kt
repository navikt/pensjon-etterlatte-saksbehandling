
import no.nav.etterlatte.beregning.grunnlag.Prosent
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProsentTest {
    @Test
    fun `foerti minus ti er tretti`() {
        Assertions.assertEquals(Prosent(40).minus(Prosent(10)), Prosent(30))
    }

    @Test
    fun `hundre minus hundre er null`() {
        Assertions.assertEquals(Prosent.hundre.minus(Prosent(100)), Prosent(0))
    }

    @Test
    fun `stoetter ikke negativ prosent`() {
        assertThrows<InternfeilException> { Prosent(-1) }
    }

    @Test
    fun `stoetter ikke prosent over 100`() {
        assertThrows<InternfeilException> { Prosent(101) }
    }
}
