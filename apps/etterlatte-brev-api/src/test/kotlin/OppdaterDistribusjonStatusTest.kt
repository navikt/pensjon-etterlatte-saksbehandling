
import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.db.BrevRepository
import no.nav.etterlatte.db.Status
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*

class OppdaterDistribusjonStatusTest {
    private val db = mockk<BrevRepository>(relaxed = true)
    private val inspector = TestRapid().apply { OppdaterDistribusjonStatus(this, db) }

    private val brevId = 100L
    private val journalpostResponse = JournalpostResponse("11111", journalpostferdigstilt = true)

    @BeforeEach
    fun before() = clearMocks(db)

    @Test
    @Disabled
    fun `Skal lagre ned journalpost-detaljer ved svar fra brev-distribusjon`() {
        val melding = """{
                "@event": "BREV:DISTRIBUER",
                "@brevId": $brevId,
                "@journalpostResponse": ${journalpostResponse.toJson()}
            }"""

        inspector.apply { sendTestMessage(melding) }.inspektør

        verify(exactly = 1) { db.oppdaterStatus(brevId, Status.JOURNALFOERT, journalpostResponse.toJson()) }
        verify(exactly = 1) { db.setJournalpostId(brevId, journalpostResponse.journalpostId) }
        verify(exactly = 0) { db.oppdaterStatus(brevId, Status.DISTRIBUERT, any()) }
        verify(exactly = 0) { db.setBestillingId(brevId, any()) }

        confirmVerified(db)
    }

    @Test
    fun `Skal lagre ned distribusjons-detaljer ved svar fra brev-distribusjon`() {
        val bestillingId = UUID.randomUUID().toString()
        val melding = """{
                "@event": "BREV:DISTRIBUER",
                "@brevId": $brevId,
                "@journalpostResponse": ${journalpostResponse.toJson()},
                "@bestilling_id": "$bestillingId"
            }"""

        inspector.apply { sendTestMessage(melding) }.inspektør

        verify(exactly = 0) { db.oppdaterStatus(brevId, Status.JOURNALFOERT, any()) }
        verify(exactly = 0) { db.setJournalpostId(brevId, any()) }
        verify(exactly = 1) { db.oppdaterStatus(brevId, Status.DISTRIBUERT, any()) }
        verify(exactly = 1) { db.setBestillingId(brevId, bestillingId) }

        confirmVerified(db)
    }
}
