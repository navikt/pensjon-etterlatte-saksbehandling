
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.DistribuerBrev
import no.nav.etterlatte.distribusjon.DistribusjonServiceMock
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class DistribuerBrevTest {
    private val inspector = TestRapid().apply { DistribuerBrev(this, DistribusjonServiceMock()) }

    @Test
    @Disabled
    fun `Skal distribuere brevet og svare bekreftende`() {
        val brevId = 100L
        val journalpostResponse = JournalpostResponse("11111", journalpostferdigstilt = true)
        val melding = """{
                "@event": "BREV:DISTRIBUER",
                "@brevId": $brevId,
                "@journalpostResponse": ${journalpostResponse.toJson()}
            }"""

        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        inspector.message(0).get("@event").asText() shouldBe "BREV:DISTRIBUER"
        inspector.message(0).get("@brevId").asLong() shouldBe brevId
        inspector.message(0).get("@bestilling_id").asText() shouldNotBe null
    }
}
