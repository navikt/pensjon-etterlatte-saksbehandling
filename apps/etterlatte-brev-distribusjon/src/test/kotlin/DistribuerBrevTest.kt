import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import journalpost.JournalpostService
import no.nav.etterlatte.DistribuerBrev
import no.nav.etterlatte.libs.common.brev.model.DistribusjonMelding
import no.nav.etterlatte.libs.common.journalpost.AvsenderMottaker
import no.nav.etterlatte.libs.common.journalpost.Bruker
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

class DistribuerBrevTest {
    private val service = mockk<JournalpostService>()
    private val inspector = TestRapid().apply { DistribuerBrev(this, service) }

    @Test
    fun `Skal distribuere meldingen og svare bekreftende`() {
        val mockJournalpostResponse = JournalpostResponse(journalpostId = "12345", journalpostferdigstilt = true)
        val distribusjonMelding = DistribusjonMelding(
            vedtakId = "00001",
            brevId = "1000",
            mottaker = AvsenderMottaker(id = "0101202012345"),
            bruker = Bruker(id = "0101202012345"),
            tittel = "Vi har innvilget din søknad om barnepensjon"
        )
        val melding = """{
                "@event": "BREV:DISTRIBUER",
                "payload": ${distribusjonMelding.toJson()},
                "@brevId": "${distribusjonMelding.brevId}"
            }"""

        coEvery { service.journalfoer(any()) } returns mockJournalpostResponse
        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        inspector.message(0).get("@event").asText() shouldBe "BREV:DISTRIBUER"
        inspector.message(0).get("@distribuert").asBoolean() shouldBe true
        inspector.message(0).get("@brevId").asText() shouldBe distribusjonMelding.brevId
        inspector.message(0).get("@journalpostResponse").asText() shouldBe mockJournalpostResponse.toJson()
    }
}
