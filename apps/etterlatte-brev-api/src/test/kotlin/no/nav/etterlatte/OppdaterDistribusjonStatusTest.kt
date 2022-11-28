package no.nav.etterlatte

import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.db.BrevRepository
import no.nav.etterlatte.libs.common.brev.model.BrevEventTypes
import no.nav.etterlatte.libs.common.brev.model.Status
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationIdKey
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
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
    fun `Skal lagre ned journalpost-detaljer ved svar fra brev-distribusjon`() {
        val melding = JsonMessage.newMessage(
            mapOf(
                eventNameKey to BrevEventTypes.JOURNALFOERT.toString(),
                "brevId" to brevId,
                correlationIdKey to UUID.randomUUID().toString(),
                "journalpostResponse" to journalpostResponse.toJson()
            )
        )

        inspector.apply { sendTestMessage(melding.toJson()) }.inspektør

        verify(exactly = 1) { db.oppdaterStatus(brevId, Status.JOURNALFOERT, journalpostResponse.toJson()) }
        verify(exactly = 1) { db.setJournalpostId(brevId, journalpostResponse.journalpostId) }
        verify(exactly = 0) { db.oppdaterStatus(brevId, Status.DISTRIBUERT, any()) }
        verify(exactly = 0) { db.setBestillingsId(brevId, any()) }

        confirmVerified(db)
    }

    @Test
    fun `Skal lagre ned distribusjons-detaljer ved svar fra brev-distribusjon`() {
        val bestillingsId = UUID.randomUUID().toString()
        val melding = JsonMessage.newMessage(
            mapOf(
                eventNameKey to BrevEventTypes.DISTRIBUERT.toString(),
                "brevId" to brevId,
                correlationIdKey to UUID.randomUUID().toString(),
                "journalpostResponse" to journalpostResponse.toJson(),
                "bestillingsId" to bestillingsId
            )
        )

        inspector.apply { sendTestMessage(melding.toJson()) }.inspektør

        verify(exactly = 0) { db.oppdaterStatus(brevId, Status.JOURNALFOERT, any()) }
        verify(exactly = 0) { db.setJournalpostId(brevId, any()) }
        verify(exactly = 1) { db.oppdaterStatus(brevId, Status.DISTRIBUERT, any()) }
        verify(exactly = 1) { db.setBestillingsId(brevId, bestillingsId) }

        confirmVerified(db)
    }
}