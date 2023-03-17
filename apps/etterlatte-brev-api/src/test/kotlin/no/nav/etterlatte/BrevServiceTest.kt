package no.nav.etterlatte

import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.distribusjon.DistribusjonService
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.pdf.PdfGeneratorKlient
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BrevServiceTest {

    private val mockkDb = mockk<BrevRepository>()
    private val mockPdfGen = mockk<PdfGeneratorKlient>()
    private val adresseService = mockk<AdresseService>()
    private val distribusjonsService = mockk<DistribusjonService>()

    private val service = BrevService(mockkDb, mockPdfGen, adresseService)

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(mockkDb, mockPdfGen, adresseService, distribusjonsService)
    }

    @Test
    fun `Hent brev innhold`() {
        val innhold = BrevInnhold("mal", Spraak.NB, "data".toByteArray())

        every { mockkDb.hentBrevInnhold(any()) } returns innhold

        val faktiskInnhold = service.hentBrevInnhold(1)

        assertEquals(innhold, faktiskInnhold)

        verify(exactly = 1) { mockkDb.hentBrevInnhold(1) }
        verify {
            listOf(mockPdfGen, adresseService, distribusjonsService) wasNot Called
        }
    }
}