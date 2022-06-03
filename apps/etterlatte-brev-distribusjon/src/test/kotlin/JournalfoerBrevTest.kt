import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.JournalfoerBrev
import no.nav.etterlatte.libs.common.brev.model.DistribusjonMelding
import no.nav.etterlatte.libs.common.journalpost.AvsenderMottaker
import no.nav.etterlatte.libs.common.journalpost.Bruker
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.util.*

class JournalfoerBrevTest {
    private val inspector = TestRapid().apply { JournalfoerBrev(this, JournalpostServiceMock()) }

    @Test
    fun `Skal journalfoere brevet og svare bekreftende`() {
        val distribusjonMelding = DistribusjonMelding(
            vedtakId = "00001",
            brevId = 1000L,
            mottaker = AvsenderMottaker(id = "0101202012345"),
            bruker = Bruker(id = "0101202012345"),
            tittel = "Vi har innvilget din søknad om barnepensjon",
            brevKode = "XX.YY-ZZ",
            journalfoerendeEnhet = "1234"
        )
        val melding = JsonMessage.newMessage(
            mapOf(
                "@event" to "BREV:DISTRIBUER",
                "@brevId" to distribusjonMelding.brevId,
                "@correlation_id" to UUID.randomUUID().toString(),
                "payload" to distribusjonMelding.toJson()
            )
        )
        val inspector = inspector.apply { sendTestMessage(melding.toJson()) }.inspektør

        inspector.message(0).get("@event").asText() shouldBe "BREV:DISTRIBUER"
        inspector.message(0).get("@brevId").asLong() shouldBe distribusjonMelding.brevId
        objectMapper.readValue<JournalpostResponse>(inspector.message(0).get("@journalpostResponse").asText()).let {
            it.journalpostId shouldNotBe null
            it.journalpoststatus shouldBe "OK"
            it.journalpostferdigstilt shouldBe true
        }
    }
}
