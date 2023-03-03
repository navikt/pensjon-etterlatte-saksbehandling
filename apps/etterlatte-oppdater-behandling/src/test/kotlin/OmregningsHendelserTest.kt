
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.BehandlingsService
import no.nav.etterlatte.OmregningsHendelser
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import rapidsandrivers.OMREGNING_ID_KEY
import java.io.FileNotFoundException
import java.util.UUID

internal class OmregningsHendelserTest {

    private val behandlingService = mockk<BehandlingsService>()
    private val inspector = TestRapid().apply { OmregningsHendelser(this, behandlingService) }

    @Test
    fun `skal opprette omregning`() {
        val omregningshendelseSlot = slot<Omregningshendelse>()
        val uuid = UUID.randomUUID()

        val returnValue = mockk<HttpResponse>().also {
            every {
                runBlocking { it.body<UUID>() }
            } returns uuid
        }
        every { behandlingService.opprettOmregning(capture(omregningshendelseSlot)) }.returns(returnValue)

        val inspector = inspector.apply { sendTestMessage(fullMelding) }

        inspector.sendTestMessage(fullMelding)

        Assertions.assertEquals(1, omregningshendelseSlot.captured.sakId)
        Assertions.assertEquals(2, inspector.inspektør.size)
        Assertions.assertEquals(uuid.toString(), inspector.inspektør.message(1).get(OMREGNING_ID_KEY).asText())
    }

    companion object {
        val fullMelding = readFile("/omregningshendelse.json")
    }
}

fun readFile(file: String) = OmregningsHendelserTest::class.java.getResource(file)?.readText()
    ?: throw FileNotFoundException("Fant ikke filen $file")