package no.nav.etterlatte.brev.distribusjon

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Mottaker
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class DistribusjonServiceTest {
    private val mockKlient = mockk<DistribusjonKlient>()
    private val service = DistribusjonServiceImpl(mockKlient)

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(mockKlient)
    }

    @Test
    fun `Distribusjon fungerer som forventet`() {
        val forventetRespons = DistribuerJournalpostResponse(UUID.randomUUID().toString())

        coEvery { mockKlient.distribuerJournalpost(any()) } returns forventetRespons

        val brevId = 1L
        val journalpostId = UUID.randomUUID().toString()
        val type = DistribusjonsType.VEDTAK
        val mottaker = opprettMottaker(journalpostId)

        val faktiskRespons = service.distribuerJournalpost(brevId, type, mottaker)

        assertEquals(forventetRespons, faktiskRespons)

        val requestSlot = slot<DistribuerJournalpostRequest>()
        coVerify(exactly = 1) { mockKlient.distribuerJournalpost(capture(requestSlot)) }
        val faktiskRequest = requestSlot.captured

        assertEquals(journalpostId, faktiskRequest.journalpostId)
        assertEquals("EY", faktiskRequest.bestillendeFagsystem)
        assertEquals(type, faktiskRequest.distribusjonstype)
        assertEquals(DistribusjonsTidspunktType.KJERNETID, faktiskRequest.distribusjonstidspunkt)
        assertEquals("etterlatte-brev-api", faktiskRequest.dokumentProdApp)
    }

    private fun opprettMottaker(journalpostId: String? = null) =
        Mottaker(
            id = UUID.randomUUID(),
            navn = "Brev",
            adresse = opprettAdresse(),
            journalpostId = journalpostId,
        )

    private fun opprettAdresse() =
        Adresse(
            adresseType = "NORSKPOSTADRESSE",
            adresselinje1 = "Fyrstikkaleen 1",
            postnummer = "1234",
            poststed = "Oslo",
            land = "Norge",
            landkode = "NOR",
        )
}
