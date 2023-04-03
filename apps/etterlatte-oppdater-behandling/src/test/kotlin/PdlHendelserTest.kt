import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.BehandlingServiceImpl
import no.nav.etterlatte.PdlHendelser
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PdlHendelserTest {

    private val behandlingService = mockk<BehandlingServiceImpl>()
    private val inspector: TestRapid = TestRapid().apply { PdlHendelser(this, behandlingService) }

    @Test
    fun `skal lytte paa doedshendelse fra pdl og kalle paa behandling`() {
        val doedshendelse = slot<Doedshendelse>()
        every { behandlingService.sendDoedshendelse(capture(doedshendelse)) } returns Unit
        inspector.apply { sendTestMessage(readFile("/doedshendelse.json")) }

        assertEquals("70078749472", doedshendelse.captured.avdoedFnr)
        assertEquals(LocalDate.of(2022, 1, 1), doedshendelse.captured.doedsdato)
        assertEquals(Endringstype.OPPRETTET, doedshendelse.captured.endringstype)
    }

    @Test
    fun `skal lytte paa utflytting-hendelse fra pdl og kalle paa etterlatte-behandling`() {
        val utflyttingHendelse = slot<UtflyttingsHendelse>()
        every { behandlingService.sendUtflyttingshendelse(capture(utflyttingHendelse)) } returns Unit
        inspector.apply { sendTestMessage(readFile("/utflyttingHendelse.json")) }

        assertEquals("70078749472", utflyttingHendelse.captured.fnr)
        assertEquals("Sverige", utflyttingHendelse.captured.tilflyttingsLand)
        assertEquals(null, utflyttingHendelse.captured.tilflyttingsstedIUtlandet)
        assertEquals(LocalDate.of(2022, 8, 8), utflyttingHendelse.captured.utflyttingsdato)
        assertEquals(Endringstype.OPPRETTET, utflyttingHendelse.captured.endringstype)
    }

    @Test
    fun `skal lytte paa forelder-barn-relasjon-hendelse fra pdl og kalle paa etterlatte-behandling`() {
        val forelderBarnRelasjonHendelse = slot<ForelderBarnRelasjonHendelse>()
        every { behandlingService.sendForelderBarnRelasjonHendelse(capture(forelderBarnRelasjonHendelse)) } returns Unit
        inspector.apply { sendTestMessage(readFile("/forelderbarnrelasjonHendelse.json")) }

        assertEquals("22028605009", forelderBarnRelasjonHendelse.captured.fnr)
        assertEquals("24091258702", forelderBarnRelasjonHendelse.captured.relatertPersonsIdent)
        assertEquals("BARN", forelderBarnRelasjonHendelse.captured.relatertPersonsRolle)
        assertEquals("MOR", forelderBarnRelasjonHendelse.captured.minRolleForPerson)
        assertEquals(null, forelderBarnRelasjonHendelse.captured.relatertPersonUtenFolkeregisteridentifikator)
        assertEquals(Endringstype.OPPRETTET, forelderBarnRelasjonHendelse.captured.endringstype)
    }
}