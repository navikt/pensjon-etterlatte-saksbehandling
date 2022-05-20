
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.DistribuerBrev
import no.nav.etterlatte.distribusjon.DistribusjonServiceMock
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

class DistribuerBrevTest {
    private val inspector = TestRapid().apply { DistribuerBrev(this, DistribusjonServiceMock()) }

    @Test
    fun `Skal distribuere brevet og svare bekreftende`() {
        val brevId = 100L
        val journalpostResponse = JournalpostResponse("11111", journalpostferdigstilt = true)
        val melding = JsonMessage.newMessage(
            mapOf(
                "@event" to "BREV:DISTRIBUER",
                "@brevId" to brevId,
                "@journalpostResponse" to journalpostResponse.toJson()
        ))
        val inspector = inspector.apply { sendTestMessage(melding.toJson()) }.inspektør

        inspector.message(0).get("@event").asText() shouldBe "BREV:DISTRIBUER"
        inspector.message(0).get("@brevId").asLong() shouldBe brevId
        inspector.message(0).get("@bestillingId").asText() shouldNotBe null
    }
}
