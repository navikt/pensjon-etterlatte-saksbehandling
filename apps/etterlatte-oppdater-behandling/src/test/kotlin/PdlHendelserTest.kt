import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.BehandlingsService
import no.nav.etterlatte.PdlHendelser
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PdlHendelserTest {


    private val behandlingService = mockk<BehandlingsService>()
    private val inspector = TestRapid().apply { PdlHendelser(this, behandlingService) }

    @Test
    fun `skal lytte paa doedshendelse fra pdl og opprette ny behandling`() {
        val doedshendelse = slot<Doedshendelse>()
        every { behandlingService.sendDoedshendelse(capture(doedshendelse)) } returns Unit

        val inspector = inspector.apply { sendTestMessage(doedshendelse_hendelse) }

        inspector.sendTestMessage(doedshendelse_hendelse)

        Assertions.assertEquals("70078749472", doedshendelse.captured.avdoedFnr)
        Assertions.assertEquals(LocalDate.of(2022, 1, 1), doedshendelse.captured.doedsdato)
        Assertions.assertEquals("OPPRETTET", doedshendelse.captured.endringstype.name)
    }


    companion object {
        val doedshendelse_hendelse = readFile("/doedshendelse.json")
    }
}