import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.Behandling
import no.nav.etterlatte.StartBehandlingAvSoeknad
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class StartBehandlingAvSoeknadTest{

    @Test
    fun test(){
        val rapid = TestRapid()
        val behandlingservice = mockk<Behandling>()

        every { behandlingservice.skaffSak("20110875720", "BARNEPENSJON") } returns 1
        every { behandlingservice.initierBehandling(1L, any(), any()) } returns UUID.randomUUID()
        StartBehandlingAvSoeknad(rapid, behandlingservice)
        val hendelseJson = javaClass.getResource("/fullMessage2.json")!!.readText()

        rapid.sendTestMessage(hendelseJson)

        assertEquals(1, rapid.inspektør.size)
        assertEquals(1, rapid.inspektør.message(0)["@sak_id"].longValue())

    }
}