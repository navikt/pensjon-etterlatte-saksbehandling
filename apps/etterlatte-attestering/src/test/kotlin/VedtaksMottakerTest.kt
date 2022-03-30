import no.nav.etterlatte.attestering.VedtaksMottaker
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class VedtaksMottakerTest {


    internal class VedtaksMottakerTest {

        private val inspector = TestRapid().also { VedtaksMottaker(it) }

        @Test
        fun `sjekk mottak av vedtak`() {

            val inspector = inspector.apply { sendTestMessage(FATTET_VEDTAK) }.inspektør
            Assertions.assertEquals("true", inspector.message(0).get("@vedtak_attestert").asText())
        }

        companion object {
            val FATTET_VEDTAK = readFile("vedtak.json")
        }
    }

}