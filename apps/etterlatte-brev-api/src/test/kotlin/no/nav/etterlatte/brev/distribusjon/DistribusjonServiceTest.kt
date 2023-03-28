package no.nav.etterlatte.brev.distribusjon

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class DistribusjonServiceTest {
    private val mockKlient = mockk<DistribusjonKlient>()
    private val mockDb = mockk<BrevRepository>(relaxed = true)

    private val service = DistribusjonServiceImpl(mockKlient, mockDb)

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(mockKlient, mockDb)
    }

    @Test
    fun `Distribusjon fungerer som forventet`() {
        val distribusjonResponse = DistribuerJournalpostResponse(UUID.randomUUID().toString())

        coEvery { mockKlient.distribuerJournalpost(any()) } returns distribusjonResponse

        val brevId = 1L
        val journalpostId = UUID.randomUUID().toString()
        val type = DistribusjonsType.VEDTAK
        val tidspunkt = DistribusjonsTidspunktType.KJERNETID

        val bestillingsID = service.distribuerJournalpost(brevId, journalpostId, type, tidspunkt, null)

        assertEquals(distribusjonResponse.bestillingsId, bestillingsID)

        val requestSlot = slot<DistribuerJournalpostRequest>()
        coVerify(exactly = 1) { mockKlient.distribuerJournalpost(capture(requestSlot)) }
        val faktiskRequest = requestSlot.captured

        assertEquals(journalpostId, faktiskRequest.journalpostId)
        assertEquals("EY", faktiskRequest.bestillendeFagsystem)
        assertEquals(type, faktiskRequest.distribusjonstype)
        assertEquals(tidspunkt, faktiskRequest.distribusjonstidspunkt)
        assertEquals("etterlatte-brev-api", faktiskRequest.dokumentProdApp)

        verify(exactly = 1) {
            mockDb.setBestillingsId(brevId, bestillingsID)
            mockDb.oppdaterStatus(brevId, Status.DISTRIBUERT, distribusjonResponse.toJson())
        }
    }
}