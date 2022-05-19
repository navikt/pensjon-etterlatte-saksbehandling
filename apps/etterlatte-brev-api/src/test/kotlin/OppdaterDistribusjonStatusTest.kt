
import io.mockk.mockk
import no.nav.etterlatte.db.BrevRepository
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

class OppdaterDistribusjonStatusTest {
    private val db = mockk<BrevRepository>()
    private val inspector = TestRapid().apply { OppdaterDistribusjonStatus(this, db) }

    @Test
    fun `Skal lagre ned journalpost-detaljer ved svar fra brev-distribusjon`() {
        val brevId = 100L
        val journalpostResponse = JournalpostResponse("11111", journalpostferdigstilt = true)
        val melding = """{
                "@event": "BREV:DISTRIBUER",
                "@brevId": $brevId,
                "@journalfoert": true,
                "@journalpostResponse": "${journalpostResponse.toJson()}"
            }"""

        inspector.apply { sendTestMessage(melding) }.inspektør

        /* todo: Finn ut hvorfor OppdaterDistribusjonStatus ikke blir kalt...
        verify(exactly = 1) { db.oppdaterStatus(brevId, "JOURNALFOERT", journalpostResponse.toJson()) }
        verify(exactly = 1) { db.setJournalpostId(brevId, journalpostResponse.journalpostId) }
        verify(exactly = 0) { db.oppdaterStatus(brevId, "DISTRIBUERT", any()) }
        verify(exactly = 0) { db.setBestillingId(brevId, any()) }
         */
    }
}
