package no.nav.etterlatte

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.adresse.AdresseKlient
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.db.BrevRepository
import no.nav.etterlatte.grunnbeloep.GrunnbeloepKlient
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.libs.common.brev.model.Brev
import no.nav.etterlatte.libs.common.brev.model.Status
import no.nav.etterlatte.norg2.Norg2Klient
import no.nav.etterlatte.pdf.PdfGeneratorKlient
import no.nav.etterlatte.vedtak.VedtakServiceImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class BrevServiceTest {

    private val mockkDb = mockk<BrevRepository>()
    private val mockPdfGen = mockk<PdfGeneratorKlient>()
    private val vedtakServiceMock = mockk<VedtakServiceImpl>()
    private val grunnlagService = mockk<GrunnlagService>()
    private val adresseService = mockk<AdresseKlient>()
    private val norg2Klient = mockk<Norg2Klient>()
    private val grunnbeloepKlientMock = mockk<GrunnbeloepKlient>()
    private val sendToRapid = mockk<(String) -> Unit>()

    private val service = BrevService(
        mockkDb,
        mockPdfGen,
        vedtakServiceMock,
        grunnlagService,
        norg2Klient,
        grunnbeloepKlientMock,
        adresseService,
        sendToRapid
    )

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
