import no.nav.etterlatte.domain.Attestasjon
import no.nav.etterlatte.attestering.VedtaksMottaker
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class VedtaksMottakerTest {


    internal class VedtaksMottakerTest {

        private val inspector = TestRapid().also { VedtaksMottaker(it) }

        @Test
        fun `sjekk mottak av vedtak og at publisert pakke har korrekt innhold`() {

            val inspektør = inspector.apply { sendTestMessage(FATTET_VEDTAK) }.inspektør
            val attestasjonTekst = inspektør.message(0).get("@attestasjon").toString()
            //println("Attestasjonstekst: \n\n $attestasjonTekst")
            val attestasjonObj = objectMapper.readValue(attestasjonTekst, Attestasjon::class.java)
            assertEquals(attestasjonObj.attestantId, "Z123456")
            assertNull(attestasjonObj.ugyldigFraDato)

        }

        companion object {
            val FATTET_VEDTAK = readFile("vedtak.json")
        }
    }

}