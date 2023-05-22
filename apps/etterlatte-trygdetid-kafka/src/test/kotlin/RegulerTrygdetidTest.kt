import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.trygdetid.kafka.TrygdetidService
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.util.*

internal class RegulerTrygdetidTest {

    private val trygdetidService = mockk<TrygdetidService>()
    private val inspector = TestRapid().apply { RegulerTrygdetid(this, trygdetidService) }

    @Test
    fun `skal opprette regulering av trygdetid`() {
        val behandlingId = slot<UUID>()
        val forrigeBehandling = slot<UUID>()
        every { trygdetidService.regulerTrygdetid(capture(behandlingId), capture(forrigeBehandling)) } returns mockk()

        inspector.apply { sendTestMessage(fullMelding) }

        behandlingId.captured shouldBe UUID.fromString("11bf9683-4edb-403c-99da-b6ec6ff7fc31")
        forrigeBehandling.captured shouldBe UUID.fromString("1be3d0dd-97be-4ccb-a71c-b3254ce7ae0a")
        inspector.inspektør.size shouldBe 1
    }

    companion object {
        val fullMelding = readFile("/omregningshendelse.json")
    }
}

fun readFile(file: String) = RegulerTrygdetidTest::class.java.getResource(file)?.readText()
    ?: throw FileNotFoundException("Fant ikke filen $file")