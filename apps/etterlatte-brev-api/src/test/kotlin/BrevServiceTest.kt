import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.BrevService
import no.nav.etterlatte.db.BrevRepository
import no.nav.etterlatte.libs.common.brev.model.Brev
import no.nav.etterlatte.libs.common.brev.model.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import pdf.PdfGeneratorKlient
import vedtak.VedtakServiceMock

internal class BrevServiceTest {

    private val mockkDb = mockk<BrevRepository>()
    private val mockPdfGen = mockk<PdfGeneratorKlient>()
    private val sendToRapid = mockk<(String) -> Unit>()

    private val service = BrevService(mockkDb, mockPdfGen, VedtakServiceMock(), sendToRapid)

    @Test
    @Disabled("Dette er ikke lengre relevant.")
    fun `Hent alle brev, ingen brev, skal opprette for vedtak`() {
        every { mockkDb.hentBrevForBehandling(any()) } returns emptyList()
        every { mockkDb.opprettBrev(any()) } returns Brev(1, "1", "tittel", Status.OPPRETTET, mockk(), false)
        coEvery { mockPdfGen.genererPdf(any()) } returns "viktig dokument".toByteArray()

        val alleBrev = runBlocking { service.hentAlleBrev("1") }

        assertEquals(1, alleBrev.size)

        verify(exactly = 1) { mockkDb.hentBrevForBehandling("1") }
        verify(exactly = 1) { mockkDb.opprettBrev(any()) }
        coVerify(exactly = 1) { mockPdfGen.genererPdf(any()) }
    }
}
