import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.BehandlingServiceImpl
import no.nav.etterlatte.OmregningsHendelser
import no.nav.etterlatte.OpprettOmregningResponse
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.BEHANDLING_VI_OMREGNER_FRA_KEY
import java.io.FileNotFoundException
import java.util.UUID

internal class OmregningsHendelserTest {
    private val behandlingService = mockk<BehandlingServiceImpl>()
    private val inspector = TestRapid().apply { OmregningsHendelser(this, behandlingService) }

    @Test
    fun `skal opprette omregning`() {
        val omregningshendelseSlot = slot<Omregningshendelse>()
        val behandlingId = UUID.randomUUID()
        val behandlingViOmregnerFra = UUID.randomUUID()

        val returnValue = OpprettOmregningResponse(behandlingId, behandlingViOmregnerFra, SakType.BARNEPENSJON)

        every { behandlingService.opprettOmregning(capture(omregningshendelseSlot)) }.returns(returnValue)

        val inspector = inspector.apply { sendTestMessage(fullMelding) }

        inspector.sendTestMessage(fullMelding)

        Assertions.assertEquals(1, omregningshendelseSlot.captured.sakId)
        Assertions.assertEquals(2, inspector.inspektør.size)
        Assertions.assertEquals(behandlingId.toString(), inspector.inspektør.message(1).get(BEHANDLING_ID_KEY).asText())
        Assertions.assertEquals(
            behandlingViOmregnerFra.toString(),
            inspector.inspektør.message(1).get(
                BEHANDLING_VI_OMREGNER_FRA_KEY,
            ).asText(),
        )
    }

    companion object {
        val fullMelding = readFile("/omregningshendelse.json")
    }
}

fun readFile(file: String) =
    OmregningsHendelserTest::class.java.getResource(file)?.readText()
        ?: throw FileNotFoundException("Fant ikke filen $file")
